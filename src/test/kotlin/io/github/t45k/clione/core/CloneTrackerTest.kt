package io.github.t45k.clione.core

import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.controller.PullRequestController
import io.github.t45k.clione.controller.cloneDetector.NiCadController
import io.github.t45k.clione.core.config.Language
import io.github.t45k.clione.core.config.RunningConfig
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.util.generatePRMock
import io.github.t45k.clione.util.toRealPath
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CloneTrackerTest {

    companion object {
        private const val REPOSITORY_FULL_NAME: String = "T45K/trial"
        private const val OLD_COMMIT_HASH = "f303aa58a4883781abec378ea435b524744c893c"
        private const val NEW_COMMIT_HASH = "49e312f1866d6d00432d6eed8f4904b65c8aba3e"
    }

    @Test
    fun test() {
        val config = RunningConfig("src", Language.JAVA, similarity = 7)
        val pullRequest: PullRequestController =
            generatePRMock(REPOSITORY_FULL_NAME, 0, NEW_COMMIT_HASH)
        val git = GitController.cloneIfNotExists(REPOSITORY_FULL_NAME, "", pullRequest)
        val cloneDetector = NiCadController(git.getProjectPath().resolve(config.src), config)
        val changedFile = setOf(
            "./storage/T45K/trial_0/src/Sample.java".toRealPath(),
            git.getProjectPath().resolve("src/Sample.java").toRealPath()
        )

        git.checkout(NEW_COMMIT_HASH)
        val newFileCache: MutableMap<Path, List<String>> = mutableMapOf()
        val (_, newIdCloneMap) = cloneDetector.collectResult(changedFile, CloneStatus.ADD, newFileCache)
        cloneDetector.parseCandidateXML(newFileCache, changedFile)
            .forEach { candidate -> newIdCloneMap.computeIfAbsent(candidate.id) { candidate } }
        val newFileClonesMap = newIdCloneMap.values.groupBy { it.filePath }

        git.checkout(OLD_COMMIT_HASH)
        val oldFileCache: MutableMap<Path, List<String>> = mutableMapOf()
        val (oldCloneSets, oldIdCloneMap) = cloneDetector.collectResult(changedFile, CloneStatus.DELETE, oldFileCache)
        val oldFileClonesMap = oldIdCloneMap.values.groupBy { it.filePath }

        val cloneTracker = CloneTracker(git, pullRequest, config)
        cloneTracker.mapClones(oldFileClonesMap, newFileClonesMap, changedFile, OLD_COMMIT_HASH, NEW_COMMIT_HASH)
        val oldInconsistentChangedClones = cloneTracker.filterTargetClones(oldCloneSets, oldIdCloneMap)

        assertEquals(1, oldInconsistentChangedClones.size)

        git.close()
    }
}
