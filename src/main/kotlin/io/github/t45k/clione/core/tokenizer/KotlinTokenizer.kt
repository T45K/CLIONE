package io.github.t45k.clione.core.tokenizer

import org.jetbrains.kotlin.spec.grammar.tools.tokenizeKotlinCode

class KotlinTokenizer : Tokenizer {
    override fun tokenize(contents: String): List<String> =
        tokenizeKotlinCode(contents)
            .map { it.text }
}
