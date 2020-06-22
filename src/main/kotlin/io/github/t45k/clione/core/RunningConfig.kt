package io.github.t45k.clione.core

import io.github.t45k.clione.controller.cloneDetector.sourcerercc.JavaSCCBlockExtractor
import io.github.t45k.clione.controller.cloneDetector.sourcerercc.KotlinSCCBlockExtractor
import io.github.t45k.clione.controller.cloneDetector.sourcerercc.SCCBlockExtractor
import io.github.t45k.clione.core.tokenizer.JDTTokenizer
import io.github.t45k.clione.core.tokenizer.KotlinTokenizer
import io.github.t45k.clione.core.tokenizer.Tokenizer

data class RunningConfig(
    val infix: String = DEFAULT_INFIX,
    val lang: Language = DEFAULT_LANG,
    val cloneDetector: CloneDetector = DEFAULT_CLONE_DETECTOR,
    val granularity: Granularity = DEFAULT_GRANULARITY,
    val similarity: Int = DEFAULT_SIMILARITY
) {
    companion object {
        const val DEFAULT_INFIX = "src"
        val DEFAULT_LANG = Language.JAVA
        val DEFAULT_CLONE_DETECTOR = CloneDetector.SOURCERERCC
        val DEFAULT_GRANULARITY = Granularity.BLOCK
        const val DEFAULT_SIMILARITY = 8
    }
}

enum class Language(
    private val string: String,
    val extension: String,
    val tokenizer: Tokenizer,
    val blockExtractor: SCCBlockExtractor // for SCC
) {
    JAVA("java", ".java", JDTTokenizer(), JavaSCCBlockExtractor()),
    KOTLIN("kotlin", ".kt", KotlinTokenizer(), KotlinSCCBlockExtractor());

    override fun toString(): String = this.string
}

enum class CloneDetector {
    NICAD, SOURCERERCC
}

enum class Granularity {
    METHOD, BLOCK
}
