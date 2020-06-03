package util

import java.nio.file.Files
import java.nio.file.Path

fun deleteRecursive(path: Path) {
    if (Files.isDirectory(path)) {
        Files.list(path)
            .forEach { deleteRecursive(it) }
    }
    Files.delete(path)
}

fun String.toPath(): Path = Path.of(this)
