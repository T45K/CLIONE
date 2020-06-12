package io.github.t45k.clione.controller

import com.github.kusumotolab.sdl4j.util.CommandLine
import com.google.common.annotations.VisibleForTesting
import io.github.t45k.clione.core.RunningConfig
import io.github.t45k.clione.core.tokenizer.Tokenizer
import io.github.t45k.clione.entity.CloneInstance
import io.github.t45k.clione.entity.CloneSets
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.IdCloneMap
import io.github.t45k.clione.entity.NoPropertyFileExistsException
import org.jgrapht.alg.clique.BronKerboschCliqueFinder
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import util.deleteRecursive
import util.toPath
import java.nio.file.Files
import java.nio.file.Path
import java.util.ResourceBundle
import javax.xml.parsers.DocumentBuilderFactory

class NiCadController(private val sourceCodePath: Path, private val config: RunningConfig) : CloneDetectorController {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)

        private val bundle: ResourceBundle = ResourceBundle.getBundle("resource")
            ?: throw NoPropertyFileExistsException()
        private val nicadDir: Path = bundle.getString("NICAD_DIR").toPath()
    }

    private val cloneDetectionResultPath: Path = sourceCodePath.parent
        .resolve("${sourceCodePath.fileName}_blocks-blind-clones")
        .resolve("${sourceCodePath.fileName}_blocks-blind-clones-0.30.xml")
    private val cloneCandidateDataPath: Path = sourceCodePath.parent
        .resolve("${sourceCodePath.fileName}_blocks.xml")
    private val tokenizer: Tokenizer = Tokenizer.create(config.lang)

    /**
     * Execute NiCad clone detector on the new revision of the target repository.
     * This is performed as follows.
     * 1. execute NiCad clone detector
     * 2. parse result xml files
     * 3. construct CloneSets and IdCloneMap
     * 4. delete xml files generated by NiCad
     */
    override fun executeOnNewRevision(changedFiles: Set<String>): Pair<CloneSets, IdCloneMap> {
        logger.info("[START]\tclone detection on new revision")
        execute()
        val fileCache: MutableMap<String, List<String>> = mutableMapOf()
        val (cloneSets: CloneSets, idCloneMap: MutableMap<Int, CloneInstance>) = collectResult(changedFiles, CloneStatus.ADD, fileCache)
        parseCandidateXML(fileCache, changedFiles).forEach { cloneCandidate -> idCloneMap.computeIfAbsent(cloneCandidate.id) { cloneCandidate } }
        deleteNiCadResultFiles()
        logger.info("[END]\tclone detection on old revision")
        return cloneSets to idCloneMap
    }

    override fun executeOnOldRevision(changedFiles: Set<String>): Pair<CloneSets, IdCloneMap> {
        logger.info("[START]\tclone detection on old revision")
        execute()
        val fileCache: MutableMap<String, List<String>> = mutableMapOf()
        val result: Pair<CloneSets, IdCloneMap> = collectResult(changedFiles, CloneStatus.DELETE, fileCache)
        deleteNiCadResultFiles()
        logger.info("[END]\tclone detection on old revision")
        return result
    }

    private fun execute() {
        val command: Array<String> = arrayOf("./nicad6", "blocks", config.lang, sourceCodePath.toRealPath().toString(), "clione")
        val result: CommandLine.CommandLineResult = CommandLine().execute(nicadDir.toFile(), *command)
        if (!result.isSuccess) {
            throw RuntimeException(result.outputLines.joinToString("\n"))
        }
    }

    @VisibleForTesting
    fun parseCandidateXML(fileCache: MutableMap<String, List<String>>, changedFiles: Set<String>): List<CloneInstance> =
        Files.readAllLines(cloneCandidateDataPath)
            .filter { it.matches("<source file=\"[^\"]+\" startline=\"[0-9]+\" endline=\"[0-9]+\">".toRegex()) }
            .asSequence()
            .mapIndexed { index, line ->
                "<source file=\"([^\"]+)\" startline=\"([0-9]+)\" endline=\"([0-9]+)\">".toRegex()
                    .matchEntire(line)!!
                    .destructured
                    .let { (fileName, startLine, endLine) ->
                        // No need to check a stable file
                        if (!changedFiles.contains(fileName) || startLine == endLine) {
                            // dummy
                            return@mapIndexed CloneInstance("", 0, 0, 0, CloneStatus.ADD)
                        }
                        val tokenSequence: List<String> = fileCache.computeIfAbsent(fileName) { Files.readAllLines(fileName.toPath()) }
                            .subList(startLine.toInt(), endLine.toInt() - 1)
                            .joinToString("\n")
                            .let { tokenizer.tokenize(it) }
                        CloneInstance(fileName, startLine.toInt(), endLine.toInt(), index + 1, CloneStatus.ADD, tokenSequence)
                    }
            }
            .filterNot { it.fileName == "" }
            .toList()

    @VisibleForTesting
    fun collectResult(changedFiles: Set<String>, initialStatus: CloneStatus, fileCache: MutableMap<String, List<String>>)
        : Pair<CloneSets, MutableMap<Int, CloneInstance>> =
        parseResultXML()
            .map {
                convertXmlElementToCloneInstance(it.first, initialStatus, changedFiles, fileCache) to
                    convertXmlElementToCloneInstance(it.second, initialStatus, changedFiles, fileCache)
            }
            .fold(SimpleGraph<Int, DefaultEdge>(DefaultEdge::class.java) to mutableMapOf<Int, CloneInstance>(),
                { (cloneRelationGraph, idCloneMap), clonePair ->
                    cloneRelationGraph.addVertex(clonePair.first.id)
                    cloneRelationGraph.addVertex(clonePair.second.id)
                    cloneRelationGraph.addEdge(clonePair.first.id, clonePair.second.id)

                    idCloneMap[clonePair.first.id] = clonePair.first
                    idCloneMap[clonePair.second.id] = clonePair.second

                    cloneRelationGraph to idCloneMap
                })
            .let {
                BronKerboschCliqueFinder(it.first)
                    .iterator()
                    .asSequence()
                    .toList() to it.second
            }

    private fun parseResultXML(): List<Pair<Element, Element>> =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(cloneDetectionResultPath.toFile())
            .documentElement
            .getElementsByTagName("clone")
            .let { clonePairs ->
                (0 until clonePairs.length)
                    .map { clonePairs.item(it) as Element }
            }
            .map { clonePair ->
                val nodeList: NodeList = clonePair.getElementsByTagName("source")
                nodeList.item(0) as Element to nodeList.item(1) as Element
            }

    private fun convertXmlElementToCloneInstance(element: Element, initialStatus: CloneStatus, changedFiles: Set<String>,
                                                 fileCache: MutableMap<String, List<String>>): CloneInstance {
        val fileName: String = element.getAttribute("file")
        val startLine: Int = element.getAttribute("startline").toInt()
        val endLine: Int = element.getAttribute("endline").toInt()
        val id: Int = element.getAttribute("pcid").toInt()
        return if (changedFiles.contains(fileName)) {
            val tokenSequence: List<String> = fileCache.computeIfAbsent(fileName) { Files.readAllLines(fileName.toPath()) }
                .subList(startLine, endLine - 1)
                .joinToString("\n")
                .let { tokenizer.tokenize(it) }
            CloneInstance(fileName, startLine, endLine, id, initialStatus, tokenSequence)
        } else {
            // If the file that contains this clone is not changed, the clone must be stable
            CloneInstance(fileName, startLine, endLine, id, CloneStatus.STABLE)
        }
    }

    /**
     * Delete all XML file generated by NiCad
     */
    private fun deleteNiCadResultFiles() =
        Files.list(sourceCodePath.parent)
            .filter { it.fileName.toString().startsWith("${sourceCodePath.fileName}_") }
            .forEach(::deleteRecursive)
}
