package io.github.t45k.clione.entity

data class CloneInstance(
    val fileName: String,
    val startLine: Int,
    val endLine: Int,
    val id: Int,
    var status: CloneStatus,
    val tokenSequence: List<String>? = null,
    var mapperCloneInstanceId: Int = -1)
