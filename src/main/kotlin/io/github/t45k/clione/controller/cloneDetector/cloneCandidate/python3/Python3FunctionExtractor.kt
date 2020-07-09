package io.github.t45k.clione.controller.cloneDetector.cloneCandidate.python3

import Python3BaseListener
import Python3Lexer
import Python3Parser
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.AntlrCloneCandidateExtractor
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.CloneCandidatesExtractListener
import io.github.t45k.clione.core.tokenizer.Python3Tokenizer
import org.antlr.v4.runtime.ParserRuleContext

class Python3FunctionExtractor : AntlrCloneCandidateExtractor(
    { Python3Lexer(it) },
    { Python3Parser(it) },
    { (it as Python3Parser).file_input() },
    { FunctionExtractListener() },
    { Python3Tokenizer().tokenize(it) }
) {

    private class FunctionExtractListener : Python3BaseListener(), CloneCandidatesExtractListener {
        private val list = mutableListOf<Python3Parser.FuncdefContext>()

        override fun getList(): List<ParserRuleContext> = list

        override fun enterFuncdef(ctx: Python3Parser.FuncdefContext) {
            list.add(ctx)
            super.enterFuncdef(ctx)
        }
    }
}
