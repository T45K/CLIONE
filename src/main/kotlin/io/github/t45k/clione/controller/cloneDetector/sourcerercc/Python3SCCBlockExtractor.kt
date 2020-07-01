package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import Python3BaseListener
import Python3Lexer
import Python3Parser
import io.github.t45k.clione.core.tokenizer.Python3Tokenizer
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.util.toCharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.nio.file.Path

class Python3SCCBlockExtractor : SCCBlockExtractor {

    override fun extract(code: String, filePath: Path, cloneStatus: CloneStatus): List<Pair<LazyCloneInstance, String>> =
        code.toCharStream()
            .run { Python3Lexer(this) }
            .run { CommonTokenStream(this) }
            .apply { this.fill() }
            .run { Python3Parser(this) }
            .run {
                val walker = ParseTreeWalker()
                val listener = BlockExtractListener()
                walker.walk(listener, this.file_input())
                listener.list
            }
            .filter { it.stop.line - it.start.line + 1 > 3 }
            .map { compoundStmt ->
                val tokenSequence: List<String> = Python3Tokenizer().tokenize(compoundStmt.text)
                val startLine = compoundStmt.start.line
                val endLine = compoundStmt.stop.line
                LazyCloneInstance(filePath.toString(), startLine, endLine, cloneStatus, tokenSequence) to tokenSequence.joinToString(" ")
            }

    class BlockExtractListener : Python3BaseListener() {
        val list = mutableListOf<Python3Parser.Compound_stmtContext>()

        override fun enterCompound_stmt(ctx: Python3Parser.Compound_stmtContext) {
            list.add(ctx)
            super.enterCompound_stmt(ctx)
        }
    }
}
