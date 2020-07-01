package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import Python3BaseListener
import Python3Lexer
import Python3Parser
import io.github.t45k.clione.core.tokenizer.Python3Tokenizer
import org.antlr.v4.runtime.ParserRuleContext

class Python3SCCBlockExtractor : AntlrSCCBlockExtractor(
    { Python3Lexer(it) },
    { Python3Parser(it) },
    { (it as Python3Parser).file_input() },
    BlockExtractListener(),
    { Python3Tokenizer().tokenize(it) }
) {

    private class BlockExtractListener : Python3BaseListener(), SCCBlockExtractListener {
        private val list = mutableListOf<Python3Parser.Compound_stmtContext>()

        override fun getList(): List<ParserRuleContext> = list

        override fun enterCompound_stmt(ctx: Python3Parser.Compound_stmtContext) {
            list.add(ctx)
            super.enterCompound_stmt(ctx)
        }
    }
}
