package io.github.t45k.clione.util

import java.nio.file.Path

fun Path.deleteRecursively() = this.toFile().deleteRecursively()

fun String.toRealPath(): Path = Path.of(this).toRealPath()

val EMPTY_NAME_PATH: Path = Path.of("")
