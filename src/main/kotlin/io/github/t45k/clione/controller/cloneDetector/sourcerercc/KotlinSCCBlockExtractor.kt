package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import io.github.t45k.clione.entity.CloneStatus
import org.jetbrains.kotlin.spec.grammar.tools.KotlinParseTree
import org.jetbrains.kotlin.spec.grammar.tools.parseKotlinCode
import org.jetbrains.kotlin.spec.grammar.tools.tokenizeKotlinCode
import java.nio.file.Path

class KotlinSCCBlockExtractor : SCCBlockExtractor {

    /**
     * Extract clone candidates by using Kotlin/grammar-tools
     */
    override fun extract(code: String, filePath: Path, cloneStatus: CloneStatus): List<Pair<LazyCloneInstance, String>> {
        val parseTree: KotlinParseTree = parseKotlinCode(tokenizeKotlinCode(code))

        println(parseTree)
        return emptyList()
    }
}
