package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.core.Language
import io.github.t45k.clione.core.RunningConfig
import io.github.t45k.clione.entity.CloneSets
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.util.toRealPath
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SourcererCCControllerTest {

    companion object {
        private const val REPOSITORY_FULL_NAME: String = "T45K/trial"
        private const val OLD_COMMIT_HASH = "f303aa58a4883781abec378ea435b524744c893c"
    }

    @Test
    fun test() {
        val git: GitController = GitController.clone(REPOSITORY_FULL_NAME, "", 0)
        val config = RunningConfig("src", Language.JAVA)
        val cloneDetector = SourcererCCController(git.getProjectPath().resolve(config.infix), config)

        val changedFiles: Set<String> = setOf("./storage/T45K/trial_0/src/Sample.java".toRealPath().toString())
        git.checkout(OLD_COMMIT_HASH)
        val (cloneSets: CloneSets, _) = cloneDetector.execute(changedFiles, CloneStatus.DELETE)

        assertEquals(listOf(setOf(1, 4)), cloneSets)

        git.deleteRepo()
    }
}