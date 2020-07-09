package io.github.t45k.clione.controller.cloneDetector.cloneCandidate.kotlin

import KotlinLexer
import KotlinParser
import KotlinParserBaseListener
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.AntlrCloneCandidateExtractor
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.CloneCandidatesExtractListener
import io.github.t45k.clione.core.tokenizer.KotlinTokenizer
import org.antlr.v4.runtime.ParserRuleContext

class KotlinMethodExtractor : AntlrCloneCandidateExtractor(
    { KotlinLexer(it) },
    { KotlinParser(it) },
    { (it as KotlinParser).kotlinFile() },
    { MethodExtractListener() },
    { KotlinTokenizer().tokenize(it) }
) {

    private class MethodExtractListener : KotlinParserBaseListener(), CloneCandidatesExtractListener {
        private val list = mutableListOf<KotlinParser.FunctionDeclarationContext>()

        override fun getList(): List<ParserRuleContext> = list

        override fun enterFunctionDeclaration(ctx: KotlinParser.FunctionDeclarationContext) {
            list.add(ctx)
            super.enterFunctionDeclaration(ctx)
        }
    }
}
