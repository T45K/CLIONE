package io.github.t45k.clione

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ClioneApplication

fun main(args: Array<String>) {
	runApplication<ClioneApplication>(*args)
}
