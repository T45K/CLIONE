package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import KotlinLexer
import KotlinParser
import KotlinParserBaseListener
import io.github.t45k.clione.core.tokenizer.KotlinTokenizer
import org.antlr.v4.runtime.ParserRuleContext

class KotlinSCCBlockExtractor : AntlrSCCBlockExtractor(
    { KotlinLexer(it) },
    { KotlinParser(it) },
    { (it as KotlinParser).kotlinFile() },
    BlockExtractListener(),
    { KotlinTokenizer().tokenize(it) }
) {

    private class BlockExtractListener : KotlinParserBaseListener(), SCCBlockExtractListener {
        private val list = mutableListOf<KotlinParser.BlockContext>()

        override fun getList(): List<ParserRuleContext> = list

        override fun enterBlock(ctx: KotlinParser.BlockContext) {
            list.add(ctx)
            super.enterBlock(ctx)
        }
    }
}
