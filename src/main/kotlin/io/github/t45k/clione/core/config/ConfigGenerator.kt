package io.github.t45k.clione.core.config

import io.github.t45k.clione.entity.InvalidConfigSpecifiedException

abstract class ConfigGenerator {
    fun generateConfig(): RunningConfig {
        return RunningConfig(
            getSrc(),
            getLang(),
            getCloneDetector(),
            getGranularity(),
            getSimilarity(),
            getStyle()
        )
    }

    private fun getSrc(): String = getSrcString() ?: RunningConfig.DEFAULT_SRC

    private fun getLang(): Language {
        val lang = getLangString() ?: return RunningConfig.DEFAULT_LANG
        return when (lang.toLowerCase()) {
            "java" -> Language.JAVA
            "kotlin", "kt" -> Language.KOTLIN
            "python", "python3", "py" -> Language.PYTHON
            else -> throw InvalidConfigSpecifiedException("lang: $lang cannot be specified")
        }
    }

    private fun getCloneDetector(): CloneDetector {
        val cloneDetector = getCloneDetectorString() ?: return RunningConfig.DEFAULT_CLONE_DETECTOR
        return when (cloneDetector.toLowerCase()) {
            "nicad" -> CloneDetector.NICAD
            "sourcerercc", "scc" -> CloneDetector.SOURCERERCC
            else -> throw InvalidConfigSpecifiedException("clone_detector: $cloneDetector cannot be specified")
        }
    }

    private fun getGranularity(): Granularity {
        val granularity = getGranularityString() ?: return RunningConfig.DEFAULT_GRANULARITY
        return when (granularity.toLowerCase()) {
            "block" -> Granularity.BLOCK
            "method" -> Granularity.METHOD
            else -> throw InvalidConfigSpecifiedException("granularity: $granularity cannot be specified")
        }
    }

    private fun getSimilarity(): Int {
        val similarity: Int = getSimilarityInteger() ?: return RunningConfig.DEFAULT_SIMILARITY
        if (similarity < 0 || similarity > 10) {
            throw InvalidConfigSpecifiedException("Similarity must be an integer value between 0 to 10")
        }
        return similarity
    }

    private fun getStyle(): Style {
        val style = getStyleString() ?: return RunningConfig.DEFAULT_STYLE
        return when (style.toLowerCase()) {
            "full" -> Style.FULL
            "summary" -> Style.SUMMARY
            "none" -> Style.NONE
            else -> throw InvalidConfigSpecifiedException("style: $style cannot be specified")
        }
    }

    abstract fun getSrcString(): String?
    abstract fun getLangString(): String?
    abstract fun getCloneDetectorString(): String?
    abstract fun getGranularityString(): String?
    abstract fun getSimilarityInteger(): Int?
    abstract fun getStyleString(): String?
}
