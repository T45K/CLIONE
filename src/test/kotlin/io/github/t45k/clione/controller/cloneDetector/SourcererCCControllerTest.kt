package io.github.t45k.clione.controller.cloneDetector

import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.controller.PullRequestController
import io.github.t45k.clione.core.Language
import io.github.t45k.clione.core.RunningConfig
import io.github.t45k.clione.entity.CloneSets
import io.github.t45k.clione.entity.CloneStatus.DELETE
import io.github.t45k.clione.util.generatePRMock
import io.github.t45k.clione.util.toRealPath
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class SourcererCCControllerTest {

    companion object {
        private const val REPOSITORY_FULL_NAME: String = "T45K/trial"
        private const val COMMIT_HASH = "f303aa58a4883781abec378ea435b524744c893c"
    }

    @Test
    fun test() {
        val pullRequest: PullRequestController = generatePRMock(REPOSITORY_FULL_NAME, 0, COMMIT_HASH)
        val git: GitController = GitController.cloneIfNotExists(REPOSITORY_FULL_NAME, "", pullRequest)
        val config = RunningConfig("src", Language.JAVA)
        val cloneDetector = SourcererCCController(git.getProjectPath().resolve(config.src), config)

        val changedFiles: Set<Path> = setOf("./storage/T45K/trial_0/src/Sample.java".toRealPath())
        val (cloneSets: CloneSets, _) = cloneDetector.execute(changedFiles, DELETE)
        git.close()

        assertEquals(listOf(setOf(1, 2)), cloneSets)
    }

    @Test
    fun testOverlapping() {
        val pr = generatePRMock(REPOSITORY_FULL_NAME, 0, "d6f4884a0dfcb37c57b805e1f3d22115d0a76fa2")
        val git = GitController.cloneIfNotExists(REPOSITORY_FULL_NAME, "", pr)
        val config = RunningConfig("src")
        val cloneDetector = SourcererCCController(git.getProjectPath().resolve(config.src), config)

        val changedFiles: Set<Path> = setOf("./storage/T45K/trial_0/src/Sample.java".toRealPath())
        val (cloneSets: CloneSets, _) = cloneDetector.execute(changedFiles, DELETE)
        git.close()

        // Block 1 contains block 2.
        // So, these blocks should not be detected as clones.
        assertNotEquals(listOf(setOf(1, 2)), cloneSets)
    }
}
