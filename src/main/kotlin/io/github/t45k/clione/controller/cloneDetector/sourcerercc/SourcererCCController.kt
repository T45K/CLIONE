package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import com.mondego.indexbased.SearchManager
import io.github.t45k.clione.controller.cloneDetector.CloneDetectorController
import io.github.t45k.clione.core.RunningConfig
import io.github.t45k.clione.entity.CloneSets
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.IdCloneMap
import org.jgrapht.alg.clique.BronKerboschCliqueFinder
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

class SourcererCCController(private val sourceCodePath: Path, private val config: RunningConfig) : CloneDetectorController {

    companion object {
        private const val sccPropertiesLocation = "./src/main/resources/sourcerer-cc.properties"
        private const val candidateFileName = "candidates.query"
        private const val resultFileName = "candidatesclones_index_WITH_FILTER.txt"
    }

    // TODO other languages' extension
    private val fileExtension: String = when (config.lang) {
        "java" -> ".java"
        "python" -> ".py"
        else -> ""
    }

    override fun executeOnNewRevision(changedFiles: Set<String>): Pair<CloneSets, IdCloneMap> {
        TODO("Not yet implemented")
    }

    override fun executeOnOldRevision(changedFiles: Set<String>): Pair<CloneSets, IdCloneMap> {
        val idCloneMap: IdCloneMap = Files.walk(sourceCodePath).asSequence()
            .filter { it.toString().endsWith(fileExtension) }
            .map { it.toRealPath() }
            .flatMap {
                val status = if (changedFiles.contains(it.toString())) CloneStatus.DELETE else CloneStatus.STABLE
                JavaSCCBlockExtractor().extract(Files.readString(it), it, status).asSequence()
            }
            .mapIndexed { index, cloneInstance -> index to cloneInstance.setId(index) }
            .toMap()

        val sccCloneInfo = generateSCCFormat()
        Files.writeString(Path.of(sourceCodePath.toString(), "NODE", "query", candidateFileName), sccCloneInfo)
        SearchManager.main(arrayOf("init", "8", "$sourceCodePath/", sccPropertiesLocation))
        SearchManager.main(arrayOf("index", "8", "$sourceCodePath/", sccPropertiesLocation))
        collectIndexFiles()
        SearchManager.main(arrayOf("search", "8", "$sourceCodePath/", sccPropertiesLocation))
        val results: List<String> = Files.readAllLines(Path.of(sourceCodePath.toString(), "NODE", "output8.0", resultFileName))

        // TODO
        return emptyList<Set<Int>>() to idCloneMap
    }

    private fun createCloneSets(rawResults: List<String>): CloneSets =
        rawResults.map { it.split(',') }
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

    private fun generateSCCFormat(): String {
        return ""
    }

    private fun collectIndexFiles() {

    }
}
