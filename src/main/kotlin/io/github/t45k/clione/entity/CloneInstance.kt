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
)
