package io.github.t45k.clione.core.tokenizer

import KotlinLexer
import io.github.t45k.clione.util.toCharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token

class KotlinTokenizer : Tokenizer {

    companion object {
        const val DELIMITED_COMMENT = 2
        const val LINE_COMMENT = 3
    }

    /**
     * NOTE: This tokenizer also tokenize text in String literal.
     */
    override fun tokenize(contents: String): List<String> =
        contents.run { this.toCharStream() }
            .run { KotlinLexer(this) }
            .run { CommonTokenStream(this) }
            .apply { this.fill() }
            .tokens
            .filterNot { it.isComment() }
            .map { it.text }
            .filter { it.isNotBlank() && it != "<EOF>" }

    private fun Token.isComment() = this.type == DELIMITED_COMMENT || this.type == LINE_COMMENT
}
