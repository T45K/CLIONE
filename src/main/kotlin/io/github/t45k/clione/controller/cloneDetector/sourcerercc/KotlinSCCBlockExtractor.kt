package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import io.github.t45k.clione.core.tokenizer.KotlinTokenizer
import io.github.t45k.clione.entity.CloneStatus
import java.nio.file.Path
import java.util.ArrayDeque
import java.util.Deque

class KotlinSCCBlockExtractor : SCCBlockExtractor {

    /**
     * Extract clone candidates by Brute Force...
     */
    override fun extract(code: String, filePath: Path, cloneStatus: CloneStatus): List<Pair<LazyCloneInstance, String>> {
        var loc = 1
        val leftBraceQueue: Deque<Pair<Int, Int>> = ArrayDeque() // position and line number
        val candidates: MutableList<Pair<LazyCloneInstance, String>> = mutableListOf()
        code.forEachIndexed { index, c ->
            when (c) {
                '\n' -> loc++
                '{' -> leftBraceQueue.add(index to loc)
                '}' -> {
                    val (position, lineNumber) = leftBraceQueue.pop()
                    val block: String = code.substring(position, index + 1)
                    candidates.add(LazyCloneInstance(filePath.toString(), lineNumber, loc, cloneStatus, KotlinTokenizer().tokenize(block)) to block)
                }
            }
        }

        return candidates
    }
}
