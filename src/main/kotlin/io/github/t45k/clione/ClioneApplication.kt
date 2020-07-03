package io.github.t45k.clione

import io.github.t45k.clione.util.deleteRecursive
import io.github.t45k.clione.util.toRealPath
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.nio.file.Files

@SpringBootApplication
class ClioneApplication

fun main(args: Array<String>) {
    val storagePath = "storage".toRealPath()
    deleteRecursive(storagePath)
    Files.createDirectory(storagePath)
    runApplication<ClioneApplication>(*args)
}
