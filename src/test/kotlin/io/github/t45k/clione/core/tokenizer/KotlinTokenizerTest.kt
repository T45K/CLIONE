package io.github.t45k.clione.core.tokenizer

import kotlin.test.Test
import kotlin.test.assertEquals

internal class KotlinTokenizerTest {

    @Test
    fun test() {
        val code = """package io.github.t45k.clione.core.tokenizer

import KotlinLexer
import io.github.t45k.clione.util.toCharStream
import org.antlr.v4.runtime.CommonTokenStream


class KotlinTokenizer : Tokenizer {

    /**
     * NOTE: This tokenizer also tokenize text in String literal.
     */
    override fun tokenize(contents: String): List<String> =
        contents.run { this.toCharStream() }
            .run { KotlinLexer(this) }
            .run { CommonTokenStream(this) }
            .apply { this.fill() }
            .tokens
            .map { it.text }
            .filter { it.isNotBlank() && it != "<EOF>" }
            //
}
"""
        val tokenSequence = KotlinTokenizer().tokenize(code)
        assertEquals(114, tokenSequence.size)
    }
}
