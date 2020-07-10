package io.github.t45k.clione.controller.cloneDetector.cloneCandidate.cpp

import CPP14BaseListener
import CPP14Lexer
import CPP14Parser
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.AntlrCloneCandidateExtractor
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.CloneCandidatesExtractListener
import io.github.t45k.clione.core.tokenizer.CppTokenizer
import org.antlr.v4.runtime.ParserRuleContext

class CppFunctionExtractor : AntlrCloneCandidateExtractor(
    { CPP14Lexer(it) },
    { CPP14Parser(it) },
    { (it as CPP14Parser).translationunit() },
    { FunctionExtractListener() },
    { CppTokenizer().tokenize(it) }
) {

    private class FunctionExtractListener : CPP14BaseListener(), CloneCandidatesExtractListener {
        private val list = mutableListOf<CPP14Parser.FunctiondefinitionContext>()

        override fun getList(): List<ParserRuleContext> = list

        override fun enterFunctiondefinition(ctx: CPP14Parser.FunctiondefinitionContext) {
            list.add(ctx)
            super.enterFunctiondefinition(ctx)
        }
    }
}