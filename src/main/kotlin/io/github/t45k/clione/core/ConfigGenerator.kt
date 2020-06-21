package io.github.t45k.clione.core

import com.moandjiezana.toml.Toml
import io.github.t45k.clione.core.RunningConfig.Companion.DEFAULT_CLONE_DETECTOR
import io.github.t45k.clione.core.RunningConfig.Companion.DEFAULT_GRANULARITY
import io.github.t45k.clione.core.RunningConfig.Companion.DEFAULT_INFIX
import io.github.t45k.clione.core.RunningConfig.Companion.DEFAULT_LANG
import io.github.t45k.clione.entity.InvalidConfigSpecifiedException

fun generateConfig(input: String): RunningConfig {
    val toml: Toml = Toml().read(input)
    return RunningConfig(
        toml.getInfix(),
        toml.getLang(),
        toml.getCloneDetector(),
        toml.getGranularity(),
        toml.getLong("similarity").toInt()
    )
}

private fun Toml.getInfix(): String = this.getString("infix") ?: DEFAULT_INFIX

private fun Toml.getLang(): Language {
    val lang = this.getString("lang") ?: return DEFAULT_LANG
    return when (lang.toLowerCase()) {
        "java" -> Language.JAVA
        "kotlin" -> Language.KOTLIN
        else -> throw InvalidConfigSpecifiedException("lang: $lang cannot be specified")
    }
}

private fun Toml.getCloneDetector(): CloneDetector {
    val cloneDetector = this.getString("clone_detector") ?: return DEFAULT_CLONE_DETECTOR
    return when (cloneDetector.toLowerCase()) {
        "nicad" -> CloneDetector.NICAD
        "sourcerercc", "scc" -> CloneDetector.SOURCERERCC
        else -> throw InvalidConfigSpecifiedException("clone_detector: $cloneDetector cannot be specified")
    }
}

private fun Toml.getGranularity(): Granularity {
    val granularity = this.getString("granularity") ?: return DEFAULT_GRANULARITY
    return when (granularity.toLowerCase()) {
        "block" -> Granularity.BLOCK
        "method" -> Granularity.METHOD
        else -> throw InvalidConfigSpecifiedException("granularity: $granularity cannot be specified")
    }
}
