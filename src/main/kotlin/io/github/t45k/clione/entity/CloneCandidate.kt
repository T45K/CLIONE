package io.github.t45k.clione.entity

import java.nio.file.Path

data class CloneCandidate(
    val filePath: Path,
    val startLine: Int,
    val endLine: Int,
    val id: Int,
    var status: CloneStatus, // Mutable filed
    val tokenSequence: List<String> = emptyList(),
    var mapperCloneInstanceId: Int = -1 // Mutable field
) {
    fun isOverlapping(cloneCandidate: CloneCandidate): Boolean =
        this.filePath == cloneCandidate.filePath &&
            (this.startLine <= cloneCandidate.startLine && this.endLine >= cloneCandidate.endLine ||
                this.startLine >= cloneCandidate.startLine && this.endLine <= cloneCandidate.endLine)
}
