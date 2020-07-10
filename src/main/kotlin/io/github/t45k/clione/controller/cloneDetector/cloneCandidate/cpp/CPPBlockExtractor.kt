package io.github.t45k.clione.controller.cloneDetector.cloneCandidate.cpp

import CPP14BaseListener
import CPP14Lexer
import CPP14Parser
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.AntlrCloneCandidateExtractor
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.CloneCandidatesExtractListener
import io.github.t45k.clione.core.tokenizer.CPPTokenizer
import org.antlr.v4.runtime.ParserRuleContext

class CPPBlockExtractor : AntlrCloneCandidateExtractor(
    { CPP14Lexer(it) },
    { CPP14Parser(it) },
    { (it as CPP14Parser).translationunit() },
    { BlockExtractListener() },
    { CPPTokenizer().tokenize(it) }
) {

    private class BlockExtractListener : CPP14BaseListener(), CloneCandidatesExtractListener {
        private val list = mutableListOf<CPP14Parser.BlockdeclarationContext>()

        override fun getList(): List<ParserRuleContext> = list

        override fun enterBlockdeclaration(ctx: CPP14Parser.BlockdeclarationContext) {
            list.add(ctx)
            super.enterBlockdeclaration(ctx)
        }
    }
}
