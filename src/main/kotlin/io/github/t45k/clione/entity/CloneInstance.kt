package io.github.t45k.clione.entity

import java.nio.file.Path

data class CloneInstance(
    val filePath: Path,
    val startLine: Int,
    val endLine: Int,
    val id: Int,
    var status: CloneStatus,
    val tokenSequence: List<String> = emptyList(),
    var mapperCloneInstanceId: Int = -1
) {
    fun isOverlapping(cloneInstance: CloneInstance): Boolean =
        this.filePath == cloneInstance.filePath &&
            (this.startLine <= cloneInstance.startLine && this.endLine >= cloneInstance.endLine ||
                this.startLine >= cloneInstance.startLine && this.endLine <= cloneInstance.endLine)
}
