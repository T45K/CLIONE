package io.github.t45k.clione.core

import io.github.t45k.clione.core.tokenizer.JDTTokenizer
import io.github.t45k.clione.core.tokenizer.KotlinTokenizer
import io.github.t45k.clione.core.tokenizer.Tokenizer

data class RunningConfig(
    val infix: String,
    val lang: Language = Language.JAVA,
    val cloneDetector: CloneDetector = CloneDetector.SOURCERERCC,
    val granularity: Granularity = Granularity.BLOCK,
    val similarity: Int = 8
)

enum class Language(private val string: String, val extension: String, val tokenizer: Tokenizer) {
    JAVA("java", ".java", JDTTokenizer()),
    KOTLIN("kotlin", ".kt", KotlinTokenizer());

    override fun toString(): String = this.string
}

enum class CloneDetector {
    NICAD, SOURCERERCC
}

enum class Granularity {
    METHOD, BLOCK
}
