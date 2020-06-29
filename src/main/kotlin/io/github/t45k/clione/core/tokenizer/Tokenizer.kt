package io.github.t45k.clione.core.tokenizer

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams

interface Tokenizer {
    fun tokenize(contents: String): List<String>

    /**
     * Generate CharStream from String type for ANTLR
     */
    fun String.toCharStream(): CharStream = CharStreams.fromString(this)
}
