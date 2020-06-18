package io.github.t45k.clione.entity

class NoPropertyFileExistsException(override val message: String?) : RuntimeException()

class InvalidLanguageSpecifiedException(override val message: String?) : RuntimeException()
