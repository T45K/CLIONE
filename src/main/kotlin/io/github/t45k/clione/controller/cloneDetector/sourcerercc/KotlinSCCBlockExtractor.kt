package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import io.github.t45k.clione.core.tokenizer.KotlinTokenizer
import io.github.t45k.clione.entity.CloneStatus
import org.jetbrains.kotlin.spec.grammar.tools.tokenizeKotlinCode
import java.nio.file.Path
import java.util.ArrayDeque
import java.util.Deque

class KotlinSCCBlockExtractor : SCCBlockExtractor {

    /**
     * Extract clone candidates by using KotlinTokenizer
     */
    override fun extract(code: String, filePath: Path, cloneStatus: CloneStatus): List<Pair<LazyCloneInstance, String>> {
        val leftBraceQueue: Deque<Pair<Int, Int>> = ArrayDeque() // position and line number
        val candidates: MutableList<Pair<LazyCloneInstance, String>> = mutableListOf()
        var currentLoC = 1
        var currentPosition = 0
        var isInString = false

        tokenizeKotlinCode(code).asSequence()
            .forEach {
                if (it.type.endsWith("QUOTE_OPEN")) {
                    isInString = true
                } else if (it.type.endsWith("QUOTE_CLOSE")) {
                    isInString = false
                }

                if (it.type.contains("NL")) {
                    currentLoC++
                } else {
                    currentLoC += it.text.filter { c -> c == '\n' }.count()
                }

                if (it.type == "LCURL" && !isInString) {
                    leftBraceQueue.push(currentPosition to currentLoC)
                } else if (it.type == "RCURL" && !isInString) {
                    val (startPosition, startLine) = leftBraceQueue.pop()
                    if (currentLoC - startLine > 3) {
                        val tokenSequence: List<String> = KotlinTokenizer().tokenize(code.substring(startPosition, currentPosition + 1))
                        candidates.add(LazyCloneInstance(filePath.toString(), startLine, currentLoC, cloneStatus, tokenSequence) to
                            tokenSequence.joinToString(" "))
                    }
                }

                currentPosition += it.text.length
            }

        return candidates
    }
}
