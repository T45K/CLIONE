package io.github.t45k.clione.util

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams

/**
 * Generate CharStream from String type for ANTLR
 */
fun String.toCharStream(): CharStream = CharStreams.fromString(this)
