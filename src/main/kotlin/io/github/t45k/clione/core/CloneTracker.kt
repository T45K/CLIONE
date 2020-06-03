package io.github.t45k.clione.core

import io.github.t45k.clione.controller.CloneDetectorController
import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.controller.NiCadController
import io.github.t45k.clione.controller.PullRequestController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

class CloneTracker(private val gitController: GitController, private val pullRequestController: PullRequestController,
                   private val config: RunningConfig) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val sourceCodePath: Path = gitController.getProjectPath().resolve(config.infix)
    private val cloneDetectionResultPath: Path = sourceCodePath.parent
        .resolve("${sourceCodePath.fileName}_blocks-blind-clones")
        .resolve("${sourceCodePath.fileName}_blocks-blind-clones-0.30.xm")
    private val cloneCandidateDataPath: Path = sourceCodePath.parent
        .resolve("${sourceCodePath.fileName}_blocks.xml")

    fun track() {
        logger.info("[START]\tClone Tracking on ${pullRequestController.getRepositoryFullName()}/${pullRequestController.getNumber()}")
        val cloneDetectorController: CloneDetectorController = NiCadController(sourceCodePath, config)


    }
}
