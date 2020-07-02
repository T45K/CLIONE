package io.github.t45k.clione.controller.cloneDetector

import com.github.kusumotolab.sdl4j.util.CommandLine
import com.google.common.annotations.VisibleForTesting
import io.github.t45k.clione.core.Granularity
import io.github.t45k.clione.core.Language
import io.github.t45k.clione.core.RunningConfig
import io.github.t45k.clione.entity.CloneInstance
import io.github.t45k.clione.entity.CloneSets
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.IdCloneMap
import io.github.t45k.clione.entity.InvalidConfigSpecifiedException
import io.github.t45k.clione.entity.NoPropertyFileExistsException
import io.github.t45k.clione.util.EMPTY_NAME_PATH
import io.github.t45k.clione.util.deleteRecursive
import io.github.t45k.clione.util.toRealPath
import org.jgrapht.alg.clique.DegeneracyBronKerboschCliqueFinder
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.nio.file.Files
import java.nio.file.Path
import java.util.ResourceBundle
import javax.xml.parsers.DocumentBuilderFactory

class NiCadController(sourceCodePath: Path, config: RunningConfig) : AbstractCloneDetectorController(sourceCodePath, config) {
    companion object {
        private val nicadDir: Path = ResourceBundle.getBundle("resource")
            ?.getString("NICAD_DIR")?.run { Path.of(this) }
            ?: throw NoPropertyFileExistsException("resource.properties does not exist")
    }

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val cloneDetectionResultPath: Path = sourceCodePath.parent
        .resolve("${sourceCodePath.fileName}_blocks-blind-clones")
        .resolve("${sourceCodePath.fileName}_blocks-blind-clones-0.${10 - config.similarity}0.xml")
    private val cloneCandidateDataPath: Path = sourceCodePath.parent
        .resolve("${sourceCodePath.fileName}_blocks.xml")
    private val nicadConfigPath: Path = nicadDir.resolve("config/${sourceCodePath.toString().substringAfter("storage/").replace('/', '_')}_clione.cfg")

    /**
     * Execute NiCad clone detector.
     * This is performed as follows.
     * 1. execute NiCad clone detector
     * 2. parse result xml files
     * 3. construct CloneSets and IdCloneMap
     * 4. delete xml files generated by NiCad
     */
    override fun execute(changedFiles: Set<Path>, initialCloneStatus: CloneStatus): Pair<CloneSets, IdCloneMap> {
        logger.info("[START]\tClone detection")
        detectClones()
        val fileCache: MutableMap<Path, List<String>> = mutableMapOf()
        val (cloneSets: CloneSets, idCloneMap: MutableMap<Int, CloneInstance>) = collectResult(changedFiles, initialCloneStatus, fileCache)
        parseCandidateXML(fileCache, changedFiles).forEach { cloneCandidate -> idCloneMap.computeIfAbsent(cloneCandidate.id) { cloneCandidate } }
        cleanup()
        logger.info("[END]\tClone detection")
        return cloneSets to idCloneMap
    }

    private fun detectClones() {
        val granularity: String = if (config.granularity == Granularity.METHOD) "functions" else "blocks"
        val lang: String = when (config.lang) {
            Language.JAVA -> "java"
            Language.PYTHON -> "py"
            else -> throw InvalidConfigSpecifiedException("NiCad does not compatible with ${config.lang}")
        }

        val configName: String = generateCGI()
        val command: Array<String> = arrayOf("./nicad6", granularity, lang, sourceCodePath.toString(), configName)
        val result: CommandLine.CommandLineResult = CommandLine().execute(nicadDir.toFile(), *command)
        if (!result.isSuccess) {
            throw RuntimeException(result.outputLines.joinToString("\n"))
        }
    }

    @VisibleForTesting
    fun parseCandidateXML(fileCache: MutableMap<Path, List<String>>, changedFiles: Set<Path>): List<CloneInstance> =
        Files.readAllLines(cloneCandidateDataPath)
            .filter { it.matches("<source file=\"[^\"]+\" startline=\"[0-9]+\" endline=\"[0-9]+\">".toRegex()) }
            .asSequence()
            .mapIndexed { index, line ->
                "<source file=\"([^\"]+)\" startline=\"([0-9]+)\" endline=\"([0-9]+)\">".toRegex()
                    .matchEntire(line)!!
                    .destructured
                    .let { (fileName, startLine, endLine) ->
                        val filePath: Path = fileName.toRealPath()
                        // No need to check a stable file
                        if (!changedFiles.contains(filePath) || startLine == endLine) {
                            // dummy
                            return@mapIndexed CloneInstance(EMPTY_NAME_PATH, 0, 0, 0, CloneStatus.ADD)
                        }
                        val tokenSequence: List<String> = fileCache.computeIfAbsent(filePath) { Files.readAllLines(it) }
                            .subList(startLine.toInt(), endLine.toInt() - 1)
                            .joinToString("\n")
                            .let { config.lang.tokenizer.tokenize(it) }
                        CloneInstance(filePath, startLine.toInt(), endLine.toInt(), index + 1, CloneStatus.ADD, tokenSequence)
                    }
            }
            .filterNot { it.filePath === EMPTY_NAME_PATH }
            .toList()

    @VisibleForTesting
    fun collectResult(changedFiles: Set<Path>, initialStatus: CloneStatus, fileCache: MutableMap<Path, List<String>>)
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
                DegeneracyBronKerboschCliqueFinder(it.first)
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

    private fun convertXmlElementToCloneInstance(element: Element, initialStatus: CloneStatus, changedFiles: Set<Path>,
                                                 fileCache: MutableMap<Path, List<String>>): CloneInstance {
        val filePath: Path = element.getAttribute("file").toRealPath()
        val startLine: Int = element.getAttribute("startline").toInt()
        val endLine: Int = element.getAttribute("endline").toInt()
        val id: Int = element.getAttribute("pcid").toInt()
        return if (changedFiles.contains(filePath)) {
            val tokenSequence: List<String> = fileCache.computeIfAbsent(filePath) { Files.readAllLines(filePath) }
                .subList(startLine, endLine - 1)
                .joinToString("\n")
                .let { config.lang.tokenizer.tokenize(it) }
            CloneInstance(filePath, startLine, endLine, id, initialStatus, tokenSequence)
        } else {
            // If the file that contains this clone is not changed, the clone must be stable
            CloneInstance(filePath, startLine, endLine, id, CloneStatus.STABLE)
        }
    }

    /**
     * @return NiCad option name
     */
    private fun generateCGI(): String {
        val config: String = """
            threshold=0.${10 - config.similarity}
            minsize=5
            maxsize=2500
            transform=none
            rename=blind
            filter=none
            abstract=none
            normalize=none
            cluster=no
            report=no
            include=""
            exclude=""
        """.trimIndent()
        Files.createFile(nicadConfigPath)
        return Files.writeString(nicadConfigPath, config).fileName.toString().substringBeforeLast('.')
    }

    /**
     * Delete all XML file generated by NiCad
     */
    override fun cleanup() {
        Files.list(sourceCodePath.parent)
            .filter { it.fileName.toString().startsWith("${sourceCodePath.fileName}_") }
            .forEach(::deleteRecursive)
        Files.deleteIfExists(nicadConfigPath)
    }
}
