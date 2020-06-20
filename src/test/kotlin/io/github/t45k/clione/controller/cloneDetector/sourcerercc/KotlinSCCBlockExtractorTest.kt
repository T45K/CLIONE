package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.util.toPath
import kotlin.test.Test

internal class KotlinSCCBlockExtractorTest {

    @Test
    fun test() {
        val code =
            """package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import io.github.t45k.clione.entity.CloneStatus
import org.jetbrains.kotlin.spec.grammar.tools.KotlinParseTree
import org.jetbrains.kotlin.spec.grammar.tools.parseKotlinCode
import java.nio.file.Path

class KotlinSCCBlockExtractor : SCCBlockExtractor {

    /**
     * Extract clone candidates by using Kotlin/grammar-tools
     */
    override fun extract(code: String, filePath: Path, cloneStatus: CloneStatus): List<Pair<LazyCloneInstance, String>> {
        val parseTree: KotlinParseTree = parseKotlinCode(code)

        println(parseTree)
        return emptyList()
    }

    private fun walkTree(tree: KotlinParseTree) {

    }
}"""
        KotlinSCCBlockExtractor().extract(code, "".toPath(), CloneStatus.STABLE)
    }
}
