package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.util.toCharStream
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.nio.file.Path

abstract class AntlrSCCBlockExtractor(
    private val lex: (CharStream) -> TokenSource,
    private val parse: (CommonTokenStream) -> Parser,
    private val top: (Parser) -> ParseTree,
    private val instantiateListener: () -> SCCBlockExtractListener,
    private val tokenize: (String) -> List<String>) : SCCBlockExtractor {

    override fun extract(code: String, filePath: Path, cloneStatus: CloneStatus): List<Pair<LazyCloneInstance, String>> =
        code.toCharStream()
            .run { lex(this) }
            .run { CommonTokenStream(this) }
            .apply { this.fill() }
            .run { parse(this) }
            .run {
                val walker = ParseTreeWalker()
                val listener: SCCBlockExtractListener = instantiateListener()
                walker.walk(listener, top(this))
                listener.getList()
            }
            .filter { it.stop.line - it.start.line + 1 > 3 }
            .map { block ->
                val startPosition = block.start.startIndex
                val endPosition = block.stop.stopIndex
                val tokenSequence: List<String> = tokenize(code.substring(startPosition, endPosition + 1))
                val startLine = block.start.line
                val endLine = block.stop.line
                LazyCloneInstance(filePath, startLine, endLine, cloneStatus, tokenSequence) to tokenSequence.joinToString(" ")
            }
}