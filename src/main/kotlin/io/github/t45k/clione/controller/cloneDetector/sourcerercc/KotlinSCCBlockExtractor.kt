package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import KotlinLexer
import KotlinParser
import KotlinParserBaseListener
import io.github.t45k.clione.core.tokenizer.KotlinTokenizer
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.util.toCharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.nio.file.Path

class KotlinSCCBlockExtractor : SCCBlockExtractor {

    /**
     * Extract clone candidates by using KotlinTokenizer
     */
    override fun extract(code: String, filePath: Path, cloneStatus: CloneStatus): List<Pair<LazyCloneInstance, String>> =
        code.toCharStream()
            .run { KotlinLexer(this) }
            .run { CommonTokenStream(this) }
            .apply { this.fill() }
            .run { KotlinParser(this) }
            .run {
                val walker = ParseTreeWalker()
                val listener = BlockExtractListener()
                walker.walk(listener, this.kotlinFile())
                listener.list
            }
            .filter { it.stop.line - it.start.line + 1 > 3 }
            .map { block ->
                val tokenSequence: List<String> = KotlinTokenizer().tokenize(block.text)
                val startLine = block.start.line
                val endLine = block.stop.line
                LazyCloneInstance(filePath.toString(), startLine, endLine, cloneStatus, tokenSequence) to tokenSequence.joinToString(" ")
            }

    class BlockExtractListener : KotlinParserBaseListener() {
        val list = mutableListOf<KotlinParser.BlockContext>()

        override fun enterBlock(ctx: KotlinParser.BlockContext) {
            list.add(ctx)
            super.enterBlock(ctx)
        }
    }
}
