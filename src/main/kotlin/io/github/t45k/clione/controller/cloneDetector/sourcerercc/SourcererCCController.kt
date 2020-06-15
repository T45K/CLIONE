package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import io.github.t45k.clione.controller.cloneDetector.CloneDetectorController
import io.github.t45k.clione.core.RunningConfig
import io.github.t45k.clione.entity.CloneSets
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.IdCloneMap
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

class SourcererCCController(private val sourceCodePath: Path, private val config: RunningConfig) : CloneDetectorController {

    companion object {
        const val SCC_PATH = ""
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

        // TODO
        return emptyList<Set<Int>>() to idCloneMap
    }

    @Synchronized
    private fun execute() {

    }
}
