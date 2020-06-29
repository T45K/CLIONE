package io.github.t45k.clione.core

import io.github.t45k.clione.core.tokenizer.CppTokenizer
import io.github.t45k.clione.core.tokenizer.JDTTokenizer
import io.github.t45k.clione.core.tokenizer.KotlinTokenizer
import io.github.t45k.clione.core.tokenizer.Python3Tokenizer
import io.github.t45k.clione.core.tokenizer.Tokenizer

data class RunningConfig(
    val src: String = DEFAULT_SRC,
    val lang: Language = DEFAULT_LANG,
    val cloneDetector: CloneDetector = DEFAULT_CLONE_DETECTOR,
    val granularity: Granularity = DEFAULT_GRANULARITY,
    val similarity: Int = DEFAULT_SIMILARITY,
    val style: Style = DEFAULT_STYLE
) {
    companion object {
        const val DEFAULT_SRC = "src"
        val DEFAULT_LANG = Language.JAVA
        val DEFAULT_CLONE_DETECTOR = CloneDetector.NICAD
        val DEFAULT_GRANULARITY = Granularity.BLOCK
        const val DEFAULT_SIMILARITY = 8
        val DEFAULT_STYLE = Style.FULL
    }
}

enum class Language(
    private val string: String,
    val extension: String,
    val tokenizer: Tokenizer
) {
    JAVA("java", ".java", JDTTokenizer()),
    KOTLIN("kotlin", ".kt", KotlinTokenizer()),
    PYTHON("python3", ".py", Python3Tokenizer()),
    CPP("cpp", ".cpp", CppTokenizer());

    override fun toString(): String = this.string
}

enum class CloneDetector {
    NICAD, SOURCERERCC
}

enum class Granularity {
    METHOD, BLOCK
}

enum class Style {
    FULL, SUMMARY, NONE
}
