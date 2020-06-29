package io.github.t45k.clione.core.tokenizer

import antlr.python3.Python3Lexer
import io.github.t45k.clione.util.toCharStream
import org.antlr.v4.runtime.CommonTokenStream

class Python3Tokenizer : Tokenizer {
    override fun tokenize(contents: String): List<String> =
        contents.run { this.toCharStream() }
            .run { Python3Lexer(this) }
            .run { CommonTokenStream(this) }
            .apply { this.fill() }
            .tokens
            .map { it.text }
            .filter { it.isNotBlank() && it != "<EOF>" }
}
