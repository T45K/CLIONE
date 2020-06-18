package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import com.mondego.indexbased.SearchManager
import io.github.t45k.clione.controller.cloneDetector.CloneDetectorController
import io.github.t45k.clione.core.RunningConfig
import io.github.t45k.clione.entity.BagOfToken
import io.github.t45k.clione.entity.CloneInstance
import io.github.t45k.clione.entity.CloneSets
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.IdCloneMap
import io.github.t45k.clione.util.deleteRecursive
import org.jgrapht.alg.clique.BronKerboschCliqueFinder
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.absoluteValue
import kotlin.streams.asSequence

class SourcererCCController(private val sourceCodePath: Path, config: RunningConfig) : CloneDetectorController {

    companion object {
        private const val sccPropertiesLocation = "./src/main/resources/sourcerer-cc.properties"
        private const val candidateFileName = "candidates.query"
        private const val resultFileName = "candidatesclones_index_WITH_FILTER.txt"
        private const val indexFilesLocation = "NODE/index/shards"
        private const val fwdIndexFilesLocation = "NODE/fwdindex/shards"

        private val symbols = arrayOf("`", "~", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "-", "+", "=", "{", "[", "}", "]", "|", "\\", ":", ";", "\"", "'", "<", ",", ">", ".", "/", "?", " ", "\n", "\r", "\t")
        private val generatedDirs = listOf("index", "fwdindex", "NODE", "backup_output", "gtpmindex", "nodes_completed.txt")
    }

    // TODO other languages' extension
    private val fileExtension: String = when (config.lang) {
        "java" -> ".java"
        "kotlin" -> ".kt"
        "python" -> ".py"
        else -> ""
    }

    // TODO issue #19
    override fun executeOnNewRevision(changedFiles: Set<String>): Pair<CloneSets, IdCloneMap> {
        cleanup()
        SearchManager.clonesWriter = null
        SearchManager.recoveryWriter = null

        val nodeDir = Files.createDirectory(sourceCodePath.resolve("NODE"))
        Files.createDirectory(nodeDir.resolve("query"))
        val (idCloneMap: IdCloneMap, bagOfTokens: List<BagOfToken>) = Files.walk(sourceCodePath).asSequence()
            .filter { it.toString().endsWith(fileExtension) }
            .map { it.toRealPath() }
            .flatMap {
                val status = if (changedFiles.contains(it.toString())) CloneStatus.ADD else CloneStatus.STABLE
                JavaSCCBlockExtractor().extract(Files.readString(it), it, status).asSequence()
            }
            .mapIndexed { index, (candidate, block) -> (index + 1 to candidate.setId(index)) to block.toBagOfToken() }
            .fold(mutableMapOf<Int, CloneInstance>() to mutableListOf<BagOfToken>()) { acc, (indexedCloneInstance: Pair<Int, CloneInstance>, bagOfToken) ->
                acc.first.also { it[indexedCloneInstance.first] = indexedCloneInstance.second } to
                    acc.second.also { it.add(bagOfToken) }
            }

        val bagOfTokensFileContents: String = generateSCCFormat(bagOfTokens)

        // SCC execution
        Files.writeString(Path.of(sourceCodePath.toString(), "NODE", "query", candidateFileName), bagOfTokensFileContents)
        SearchManager.main(arrayOf("init", "8", "$sourceCodePath/", sccPropertiesLocation))
        SearchManager.main(arrayOf("index", "8", "$sourceCodePath/", sccPropertiesLocation))
        collectIndexFiles()
        SearchManager.main(arrayOf("search", "8", "$sourceCodePath/", sccPropertiesLocation))
        val sccResult: List<String> = Files.readAllLines(Path.of(sourceCodePath.toString(), "NODE", "output8.0", resultFileName))

        cleanup()

        return createCloneSets(sccResult) to idCloneMap
    }

    /**
     *
     * Note: SCC requires 1-indexed block information.
     * Pay attention to indexing.
     */
    override fun executeOnOldRevision(changedFiles: Set<String>): Pair<CloneSets, IdCloneMap> {
        cleanup()
        SearchManager.clonesWriter = null
        SearchManager.recoveryWriter = null

        val nodeDir = Files.createDirectory(sourceCodePath.resolve("NODE"))
        Files.createDirectory(nodeDir.resolve("query"))
        val (idCloneMap: IdCloneMap, bagOfTokens: List<BagOfToken>) = Files.walk(sourceCodePath).asSequence()
            .filter { it.toString().endsWith(fileExtension) }
            .map { it.toRealPath() }
            .flatMap {
                val status = if (changedFiles.contains(it.toString())) CloneStatus.DELETE else CloneStatus.STABLE
                JavaSCCBlockExtractor().extract(Files.readString(it), it, status).asSequence()
            }
            .mapIndexed { index, (candidate, block) -> (index + 1 to candidate.setId(index)) to block.toBagOfToken() }
            .fold(mutableMapOf<Int, CloneInstance>() to mutableListOf<BagOfToken>()) { acc, (indexedCloneInstance: Pair<Int, CloneInstance>, bagOfToken) ->
                acc.first.also { it[indexedCloneInstance.first] = indexedCloneInstance.second } to
                    acc.second.also { it.add(bagOfToken) }
            }

        val bagOfTokensFileContents: String = generateSCCFormat(bagOfTokens)

        // SCC execution
        Files.writeString(Path.of(sourceCodePath.toString(), "NODE", "query", candidateFileName), bagOfTokensFileContents)
        SearchManager.main(arrayOf("init", "8", "$sourceCodePath/", sccPropertiesLocation))
        SearchManager.main(arrayOf("index", "8", "$sourceCodePath/", sccPropertiesLocation))
        collectIndexFiles()
        SearchManager.main(arrayOf("search", "8", "$sourceCodePath/", sccPropertiesLocation))
        val sccResult: List<String> = Files.readAllLines(Path.of(sourceCodePath.toString(), "NODE", "output8.0", resultFileName))

        cleanup()

        return createCloneSets(sccResult) to idCloneMap
    }

    /**
     * Create clone sets from SCC result file contents.
     * SCC result file formats: file_id_1,clone_id_1,file_id_2,clone_id_2
     * This means clone_id_1 and clone_id_2 are clone pair.
     */
    private fun createCloneSets(sccResult: List<String>): CloneSets =
        sccResult.map { it.split(',') }
            .map { it[1].toInt() to it[3].toInt() }
            .fold(SimpleGraph<Int, DefaultEdge>(DefaultEdge::class.java)) { graph, pair ->
                graph.addVertex(pair.first)
                graph.addVertex(pair.second)
                graph.addEdge(pair.first, pair.second)
                graph
            }
            .let {
                BronKerboschCliqueFinder(it)
                    .iterator()
                    .asSequence()
                    .toList()
            }

    private fun generateSCCFormat(bagOfTokens: List<BagOfToken>): String =
        bagOfTokens.mapIndexed { index, bagOfToken ->
            val totalTokens = bagOfToken.values.sum()
            "1,${index + 1},$totalTokens,${bagOfToken.size},${bagOfToken.hashCode().absoluteValue}@#@${bagOfToken.format()}"
        }
            .joinToString("\n")

    private fun BagOfToken.format(): String =
        this.map { "${it.key}@@::@@${it.value}" }
            .joinToString(",")

    private fun collectIndexFiles() {
        val indexPath: Path = Files.createDirectory(sourceCodePath.resolve("index"))
        Files.list(sourceCodePath.resolve(indexFilesLocation))
            .forEach { Files.move(it, indexPath.resolve(it.fileName)) }
        val fwdIndexPath: Path = Files.createDirectory(sourceCodePath.resolve("fwdindex"))
        Files.list(sourceCodePath.resolve(fwdIndexFilesLocation))
            .forEach { Files.move(it, fwdIndexPath.resolve(it.fileName)) }
    }

    private fun String.toBagOfToken(): BagOfToken =
        this.split(*symbols)
            .filterNot { it.isBlank() }
            .groupBy { it }
            .map { it.key to it.value.size }
            .toMap()

    /**
     * Delete files generated by SCC
     */
    override fun cleanup() =
        generatedDirs.map(sourceCodePath::resolve)
            .forEach(::deleteRecursive)
}
