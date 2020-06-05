package io.github.t45k.clione.core.tokenizer

import org.eclipse.jdt.core.ToolFactory
import org.eclipse.jdt.core.compiler.ITerminalSymbols

class JDTTokenizer : Tokenizer {
    override fun tokenize(contents: String): List<String> =
        ToolFactory
            .createScanner(false, false, true, true)
            .also { it.source = contents.toCharArray() }
            .let { scanner ->
                generateSequence { 0 }
                    .map { scanner.nextToken }
                    .takeWhile { it != ITerminalSymbols.TokenNameEOF }
                    .map { String(scanner.rawTokenSource) }
                    .toList()
            }
}
