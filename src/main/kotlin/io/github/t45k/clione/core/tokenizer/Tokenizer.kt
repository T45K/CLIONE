package io.github.t45k.clione.core.tokenizer

import io.github.t45k.clione.core.Language
import io.github.t45k.clione.entity.InvalidLanguageSpecifiedException

interface Tokenizer {
    companion object {
        fun create(lang: Language): Tokenizer =
            when (lang) {
                Language.JAVA -> JDTTokenizer()
                Language.KOTLIN -> KotlinTokenizer()
                else -> throw InvalidLanguageSpecifiedException("$lang is invalid")
            }
    }

    fun tokenize(contents: String): List<String>
}

