package io.github.t45k.clione.core.tokenizer

import kotlin.test.Test
import kotlin.test.assertEquals

internal class KotlinTokenizerTest {

    @Test
    fun test() {
        val contents = "val str:Int = 0"
        val tokens: List<String> = KotlinTokenizer().tokenize(contents)
            .filter { it.isNotBlank() }
        assertEquals(listOf("val", "str", ":", "Int", "=", "0"), tokens)
    }
}
