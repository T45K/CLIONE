package io.github.t45k.clione.controller.cloneDetector.cloneCandidate

import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.cpp.CppBlockExtractor
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.cpp.CppFunctionExtractor
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.java.JavaBlockExtractor
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.java.JavaMethodExtractor
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.kotlin.KotlinBlockExtractor
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.kotlin.KotlinMethodExtractor
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.python3.Python3BlockExtractor
import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.python3.Python3FunctionExtractor
import io.github.t45k.clione.core.Granularity.BLOCK
import io.github.t45k.clione.core.Granularity.METHOD
import io.github.t45k.clione.core.Language.CPP
import io.github.t45k.clione.core.Language.JAVA
import io.github.t45k.clione.core.Language.KOTLIN
import io.github.t45k.clione.core.Language.PYTHON
import io.github.t45k.clione.core.RunningConfig
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.InvalidConfigSpecifiedException
import java.nio.file.Path

interface CloneCandidateExtractor {
    companion object {
        fun create(config: RunningConfig): CloneCandidateExtractor =
            when {
                config.lang == JAVA && config.granularity == BLOCK -> JavaBlockExtractor()
                config.lang == JAVA && config.granularity == METHOD -> JavaMethodExtractor()
                config.lang == KOTLIN && config.granularity == BLOCK -> KotlinBlockExtractor()
                config.lang == KOTLIN && config.granularity == METHOD -> KotlinMethodExtractor()
                config.lang == PYTHON && config.granularity == BLOCK -> Python3BlockExtractor()
                config.lang == PYTHON && config.granularity == METHOD -> Python3FunctionExtractor()
                config.lang == CPP && config.granularity == BLOCK -> CppBlockExtractor()
                config.lang == CPP && config.granularity == METHOD -> CppFunctionExtractor()

                else -> throw InvalidConfigSpecifiedException("How do you reach here?")
            }
    }

    fun extract(code: String, filePath: Path, cloneStatus: CloneStatus): List<Pair<LazyCloneInstance, String>>
}
