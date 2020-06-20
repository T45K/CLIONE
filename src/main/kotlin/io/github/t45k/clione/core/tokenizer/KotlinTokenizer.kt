package io.github.t45k.clione.core.tokenizer

import org.jetbrains.kotlin.spec.grammar.tools.tokenizeKotlinCode

@Deprecated("The library this class uses is insufficient. Please use JDTTokenizer")
class KotlinTokenizer : Tokenizer {
    override fun tokenize(contents: String): List<String> =
        tokenizeKotlinCode(contents)
            .map { it.text }
            .filter { it.isNotBlank() }
}
