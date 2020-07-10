package io.github.t45k.clione.core.tokenizer

import CPP14Lexer
import io.github.t45k.clione.util.toCharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token

class CPPTokenizer : Tokenizer {

    companion object {
        const val BLOCK_COMMENT = 149
        const val LINE_COMMENT = 150
    }

    /**
     * NOTE: This tokenizer also tokenize text in String literal.
     */
    override fun tokenize(contents: String): List<String> =
        contents.run { this.toCharStream() }
            .run { CPP14Lexer(this) }
            .run { CommonTokenStream(this) }
            .apply { this.fill() }
            .tokens
            .filterNot { it.isComment() }
            .map { it.text }
            .filter { it.isNotBlank() && it != "<EOF>" }

    private fun Token.isComment(): Boolean = this.type == BLOCK_COMMENT || this.type == LINE_COMMENT
}
