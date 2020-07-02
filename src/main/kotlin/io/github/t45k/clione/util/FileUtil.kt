package io.github.t45k.clione.util

import java.nio.file.Files
import java.nio.file.Path

fun deleteRecursive(path: Path) {
    if (Files.isDirectory(path)) {
        Files.list(path)
            .forEach { deleteRecursive(it) }
    }
    Files.deleteIfExists(path)
}

fun String.toRealPath(): Path = Path.of(this).toRealPath()

val EMPTY_NAME_PATH: Path = Path.of("")
