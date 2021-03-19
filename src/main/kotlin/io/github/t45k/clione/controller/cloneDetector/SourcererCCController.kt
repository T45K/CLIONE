package io.github.t45k.clione.controller.cloneDetector

import com.mondego.indexbased.SearchManager
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.CloneCandidateExtractor
import io.github.t45k.clione.core.config.RunningConfig
import io.github.t45k.clione.entity.BagOfToken
import io.github.t45k.clione.entity.CloneCandidate
import io.github.t45k.clione.entity.CloneSets
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.IdCloneMap
import io.github.t45k.clione.util.deleteRecursively
import org.jgrapht.alg.clique.DegeneracyBronKerboschCliqueFinder
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.absoluteValue
import kotlin.streams.asSequence

class SourcererCCController(sourceCodePath: Path, config: RunningConfig) :
    AbstractCloneDetectorController(sourceCodePath, config) {

    companion object {
        private const val SCC_PROPERTY_LOCATION = "./src/main/resources/sourcerer-cc.properties"
        private const val QUERY_FILE_LOCATION = "NODE/query/query"
        private const val SCC_RESULT_FILE_NAME = "queryclones_index_WITH_FILTER.txt"
        private const val INDEX_FILES_LOCATION = "NODE/index/shards"
        private const val FWD_INDEX_FILES_LOCATION = "NODE/fwdindex/shards"

        private val symbols = arrayOf(
            "`",
            "~",
            "!",
            "@",
            "#",
            "$",
            "%",
            "^",
            "&",
            "*",
            "(",
            ")",
            "-",
            "+",
            "=",
            "{",
            "[",
            "}",
            "]",
            "|",
            "\\",
            ":",
            ";",
            "\"",
            "'",
            "<",
            ",",
            ">",
            ".",
            "/",
            "?",
            " ",
            "\n",
            "\r",
            "\t"
        )
        private val generatedDirs =
            listOf("index", "fwdindex", "NODE", "backup_output", "gtpmindex", "nodes_completed.txt")

        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    /**
     * Execute SourcererCC clone detector
     * This is performed as follows.
     * 1. extract blocks from files under sourceCodePath
     * 2. export the above results to sourceCodePath/Node/query/candidates.query
     * 3. execute SourcererCC
     * 4. construct CloneSets and IdCloneMap from SourcererCC result file
     * 5. delete xml files generated by SourcererCC
     *
     * Note: SCC requires 1-indexed block information.
     * Pay attention to indexing.
     */
    override fun execute(changedFiles: Set<Path>, initialCloneStatus: CloneStatus): Pair<CloneSets, IdCloneMap> {
        logger.info("[START]\tClone detection")
        cleanup()

        val (idCloneMap: IdCloneMap, bagOfTokens: List<BagOfToken>) = collectCloneCandidates(
            changedFiles,
            initialCloneStatus
        )

        Files.createDirectory(sourceCodePath.resolve("NODE")).let { Files.createDirectory(it.resolve("query")) }
        Files.writeString(sourceCodePath.resolve(QUERY_FILE_LOCATION), generateSCCFormat(bagOfTokens))
        detectClones()
        val sccResult: List<String> = Files.readAllLines(
            Path.of(
                sourceCodePath.toString(),
                "NODE",
                "output${config.similarity}.0",
                SCC_RESULT_FILE_NAME
            )
        )

        cleanup()
        logger.info("[END]\tClone detection")

        return constructCloneSets(sccResult, idCloneMap) to idCloneMap
    }

    private fun collectCloneCandidates(
        changedFiles: Set<Path>,
        initialCloneStatus: CloneStatus
    ): Pair<IdCloneMap, List<BagOfToken>> =
        Files.walk(sourceCodePath).asSequence()
            .filter { it.toString().endsWith(config.lang.extension) }
            .map { it.toRealPath() }
            .flatMap {
                val status = if (changedFiles.contains(it)) initialCloneStatus else CloneStatus.STABLE
                val cloneCandidateExtractor = CloneCandidateExtractor.create(config)
                cloneCandidateExtractor.extract(Files.readString(it), it, status).asSequence()
            }
            .mapIndexed { index, (candidate, block) -> (index + 1 to candidate.setId(index)) to block.toBagOfToken() }
            .fold(mutableMapOf<Int, CloneCandidate>() to mutableListOf()) { acc: Pair<MutableMap<Int, CloneCandidate>, MutableList<BagOfToken>>,
                                                                            (indexedCloneCandidate: Pair<Int, CloneCandidate>, bagOfToken: BagOfToken) ->
                acc.first.also { it[indexedCloneCandidate.first] = indexedCloneCandidate.second } to
                    acc.second.also { it.add(bagOfToken) }
            }

    /**
     * Detail: https://github.com/T45K/SourcererCC/blob/master/clone-detector/README.md
     * Notice: SourcererCC (SearchManager) internally uses a lot of mutable static fields.
     *         So, if concurrent access occurs, the results will not be ensured.
     */
    @Synchronized
    private fun detectClones() {
        SearchManager.clonesWriter = null
        SearchManager.recoveryWriter = null
        SearchManager.main(arrayOf("init", config.similarity.toString(), "$sourceCodePath/", SCC_PROPERTY_LOCATION))
        SearchManager.main(arrayOf("index", config.similarity.toString(), "$sourceCodePath/", SCC_PROPERTY_LOCATION))
        moveIndexFiles()
        SearchManager.main(arrayOf("search", config.similarity.toString(), "$sourceCodePath/", SCC_PROPERTY_LOCATION))
    }

    /**
     * Construct clone sets from SCC result file contents.
     * SCC result file formats: file_id_1,clone_id_1,file_id_2,clone_id_2
     * This means clone_id_1 and clone_id_2 are clone pair.
     */
    private fun constructCloneSets(sccResult: List<String>, idCloneMap: IdCloneMap): CloneSets =
        sccResult.map { it.split(',') }
            .map { it[1].toInt() to it[3].toInt() }
            .filterNot { (idCloneMap[it.first] ?: error("")).isOverlapping(idCloneMap[it.second] ?: error("")) }
            .fold(SimpleGraph<Int, DefaultEdge>(DefaultEdge::class.java)) { graph, pair ->
                graph.addVertex(pair.first)
                graph.addVertex(pair.second)
                graph.addEdge(pair.first, pair.second)
                graph
            }
            .let {
                DegeneracyBronKerboschCliqueFinder(it)
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

    private fun moveIndexFiles() {
        val indexPath: Path = Files.createDirectory(sourceCodePath.resolve("index"))
        Files.list(sourceCodePath.resolve(INDEX_FILES_LOCATION))
            .forEach { Files.move(it, indexPath.resolve(it.fileName)) }
        val fwdIndexPath: Path = Files.createDirectory(sourceCodePath.resolve("fwdindex"))
        Files.list(sourceCodePath.resolve(FWD_INDEX_FILES_LOCATION))
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
            .forEach { it.deleteRecursively() }
}
