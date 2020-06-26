package io.github.t45k.clione.core

import io.github.t45k.clione.entity.InvalidConfigSpecifiedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class ConfigGeneratorKtTest {

    @Test
    fun testGenerateConfig() {
        val toml = """src = "src/main/java"
            lang = "java"
            granularity = "method"
            similarity = 9
        """.trimIndent()

        val (src, lang, cloneDetector, granularity, similarity) = generateConfig(toml)
        assertEquals("src/main/java", src)
        assertEquals(Language.JAVA, lang)
        assertEquals(CloneDetector.SOURCERERCC, cloneDetector)
        assertEquals(Granularity.METHOD, granularity)
        assertEquals(9, similarity)
    }

    @Test
    fun testGenerateConfigWithException() {
        val toml = """lang = "python""""
        assertFailsWith<InvalidConfigSpecifiedException> { generateConfig(toml) }
    }
}
