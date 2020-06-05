package io.github.t45k.clione.entity

data class FileDiff(val type: FileChangeType, val lineMapping: List<Int>, val newFileName: String)
