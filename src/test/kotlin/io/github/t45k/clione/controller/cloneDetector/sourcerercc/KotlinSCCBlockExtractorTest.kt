package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.util.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

internal class KotlinSCCBlockExtractorTest {

    @Test
    fun test() {
        val code =
            """package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import io.github.t45k.clione.core.tokenizer.JDTTokenizer
import io.github.t45k.clione.entity.CloneStatus
import org.eclipse.jdt.core.ToolFactory
import org.eclipse.jdt.core.compiler.ITerminalSymbols
import java.nio.file.Path
import java.util.ArrayDeque
import java.util.Deque

class KotlinSCCBlockExtractor : SCCBlockExtractor {

    /**
     * Extract clone candidates by using JDTTokenizer
     */
    override fun extract(code: String, filePath: Path, cloneStatus: CloneStatus): List<Pair<LazyCloneInstance, String>> {
        val leftBraceQueue: Deque<Pair<Int, Int>> = ArrayDeque() // position and line number
        val candidates: MutableList<Pair<LazyCloneInstance, String>> = mutableListOf()

        ToolFactory
            .createScanner(false, false, true, true)
            .also { it.source = code.toCharArray() }
            .let { scanner ->
                generateSequence { 0 }
                    .map { scanner.nextToken }
                    .takeWhile { it != ITerminalSymbols.TokenNameEOF }
                    .forEach {
                        if (it == ITerminalSymbols.TokenNameLBRACE) {
                            val startPosition: Int = scanner.currentTokenStartPosition
                            leftBraceQueue.add(startPosition to scanner.getLineNumber(startPosition))
                        }else if (it == ITerminalSymbols.TokenNameRBRACE) {
                            val endPosition: Int = scanner.currentTokenEndPosition
                            val endLine: Int = scanner.getLineNumber(endPosition)
                            val (startPosition, startLine) = leftBraceQueue.pop()
                            if (endLine - startLine + 1 <= 3) {
                                return@forEach
                            }
                            val tokenSequence: List<String> = JDTTokenizer().tokenize(code.substring(startPosition, endPosition + 1))
                            candidates.add(LazyCloneInstance(filePath.toString(), startLine, endLine, cloneStatus, tokenSequence) to
                                tokenSequence.joinToString(" "))
                        }
                    }
            }

        return candidates
    }
}
"""
        assertEquals(6, KotlinSCCBlockExtractor().extract(code, "".toPath(), CloneStatus.STABLE).size)
    }
}
