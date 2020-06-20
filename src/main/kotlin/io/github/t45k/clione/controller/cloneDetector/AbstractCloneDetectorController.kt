package io.github.t45k.clione.controller.cloneDetector

import io.github.t45k.clione.controller.cloneDetector.sourcerercc.SourcererCCController
import io.github.t45k.clione.core.CloneDetector
import io.github.t45k.clione.core.RunningConfig
import java.nio.file.Path

abstract class AbstractCloneDetectorController(protected val sourceCodePath: Path, protected val config: RunningConfig)
    : CloneDetectorController

fun create(sourceCodePath: Path, config: RunningConfig): CloneDetectorController =
    when (config.cloneDetector) {
        CloneDetector.NICAD -> NiCadController(sourceCodePath, config)
        CloneDetector.SOURCERERCC -> SourcererCCController(sourceCodePath, config)
    }