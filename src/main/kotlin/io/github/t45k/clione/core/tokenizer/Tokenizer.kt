package io.github.t45k.clione.core.tokenizer

interface Tokenizer {
    fun tokenize(contents: String): List<String>
}
