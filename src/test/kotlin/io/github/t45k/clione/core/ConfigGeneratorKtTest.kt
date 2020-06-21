package io.github.t45k.clione.core

import kotlin.test.Test
import kotlin.test.assertEquals

internal class ConfigGeneratorKtTest {

    @Test
    fun testGenerateConfig() {
        val toml = """infix = "src/main/java"
            lang = "java"
            granularity = "method"
            similarity = 9
        """.trimIndent()

        val (infix, lang, cloneDetector, granularity, similarity) = generateConfig(toml)
        assertEquals("src/main/java", infix)
        assertEquals(Language.JAVA, lang)
        assertEquals(CloneDetector.SOURCERERCC, cloneDetector)
        assertEquals(Granularity.METHOD, granularity)
        assertEquals(9, similarity)
    }
}
