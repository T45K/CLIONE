package io.github.t45k.clione.core

data class RunningConfig(
    val infix: String,
    val lang: Language = Language.JAVA,
    val cloneDetector: CloneDetector = CloneDetector.SOURCERERCC,
    val granularity: Granularity = Granularity.BLOCK,
    val similarity: Int = 8
)

enum class Language(private val string: String, val extension: String) {
    JAVA("java", ".java"), KOTLIN("kotlin", ".kt"), PYTHON("python", ".py");

    override fun toString(): String = this.string
}

enum class CloneDetector {
    NICAD, SOURCERERCC
}

enum class Granularity {
    METHOD, BLOCK
}
