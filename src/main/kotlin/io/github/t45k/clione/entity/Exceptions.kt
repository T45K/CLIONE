package io.github.t45k.clione.entity

class NoPropertyFileException(override val message: String?) : RuntimeException()

class InvalidConfigSpecifiedException(override val message: String?) : RuntimeException()
