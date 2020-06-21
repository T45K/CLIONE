package io.github.t45k.clione.entity

class NoPropertyFileExistsException(override val message: String?) : RuntimeException()

class InvalidConfigSpecifiedException(override val message: String?) : RuntimeException()
