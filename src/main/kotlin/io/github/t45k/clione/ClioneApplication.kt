package io.github.t45k.clione

import io.github.t45k.clione.util.deleteRecursively
import io.github.t45k.clione.util.toRealPath
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.nio.file.Files

@SpringBootApplication
class ClioneApplication

fun main(args: Array<String>) {
    Files.list("storage".toRealPath())
        .filter { it.fileName.toString() != ".gitkeep" }
        .forEach { it.deleteRecursively() }
    runApplication<ClioneApplication>(*args)
}
