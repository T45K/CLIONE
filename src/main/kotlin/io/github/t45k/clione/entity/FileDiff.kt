package io.github.t45k.clione.entity

import java.nio.file.Path

data class FileDiff(
    val type: FileChangeType,
    val addedLines: List<Int>,
    val deletedLines: List<Int>,
    val newFilePath: Path
)
