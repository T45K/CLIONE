package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import io.github.t45k.clione.entity.CloneInstance
import io.github.t45k.clione.entity.CloneStatus
import java.nio.file.Path

/**
 * Special CloneInstance class to delay giving id.
 */
data class LazyCloneInstance(
    val filePath: Path,
    val startLine: Int,
    val endLine: Int,
    var status: CloneStatus,
    val tokenSequence: List<String> = emptyList()
) {
    fun setId(id: Int): CloneInstance =
        CloneInstance(filePath, startLine, endLine, id, status, tokenSequence)
}
