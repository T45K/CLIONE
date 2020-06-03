package io.github.t45k.clione.entity

class NoPropertyFileExistsException : RuntimeException()
class InvalidLanguageSpecifiedException(override val message: String?) : RuntimeException()
