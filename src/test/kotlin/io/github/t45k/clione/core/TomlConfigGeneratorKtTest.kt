package io.github.t45k.clione.core

import io.github.t45k.clione.core.config.CloneDetector
import io.github.t45k.clione.core.config.Granularity
import io.github.t45k.clione.core.config.Language
import io.github.t45k.clione.core.config.generateConfig
import io.github.t45k.clione.entity.InvalidConfigSpecifiedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class TomlConfigGeneratorKtTest {

    @Test
    fun testGenerateConfig() {
        val toml = """src = "src/main/java"
            lang = "java"
            granularity = "method"
            similarity = 9
            clone_detector = "scc"
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
        val toml = """lang = "perl""""
        assertFailsWith<InvalidConfigSpecifiedException> { generateConfig(toml) }
    }
}
