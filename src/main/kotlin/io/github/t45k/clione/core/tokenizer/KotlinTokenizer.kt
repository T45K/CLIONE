package io.github.t45k.clione.core.tokenizer

import org.jetbrains.kotlin.spec.grammar.tools.tokenizeKotlinCode

class KotlinTokenizer : Tokenizer {

    /**
     * NOTE: This tokenizer also tokenize text in String literal.
     */
    override fun tokenize(contents: String): List<String> =
        tokenizeKotlinCode(contents)
            .filterNot { it.type.contains("WS") || it.type.contains("NL") || it.type.contains("Comment") }
            .map { it.text }
}
