package io.github.t45k.clione.core.tokenizer

import io.github.t45k.clione.entity.InvalidLanguageSpecifiedException

interface Tokenizer {
    companion object {
        fun create(lang: String): Tokenizer =
            when (lang) {
                "java" -> JDTTokenizer()
                else -> throw InvalidLanguageSpecifiedException("$lang is invalid")
            }
    }

    fun tokenize(contents: String): List<String>
}

