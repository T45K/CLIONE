package io.github.t45k.clione.controller.cloneDetector.sourcerercc

import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.core.RunningConfig
import io.github.t45k.clione.util.toRealPath
import kotlin.test.Test

internal class SourcererCCControllerTest {

    companion object {
        private const val REPOSITORY_FULL_NAME: String = "T45K/trial"
        private const val OLD_COMMIT_HASH = "f303aa58a4883781abec378ea435b524744c893c"
    }

    @Test
    fun test() {
        val git: GitController = GitController.clone(REPOSITORY_FULL_NAME, "", 0)
        val config = RunningConfig("src", "java")
        val cloneDetector = SourcererCCController(git.getProjectPath().resolve(config.infix), config)

        val changedFiles: Set<String> = setOf("./storage/T45K/trial_0/src/Sample.java".toRealPath().toString())
        val (newCloneSets, newIdCloneMap) = cloneDetector.executeOnNewRevision(changedFiles)

        git.checkout(OLD_COMMIT_HASH)
        val (oldCloneSets, oldIdCloneMap) = cloneDetector.executeOnOldRevision(changedFiles)

        println(newCloneSets)
        println(newIdCloneMap)
        println(oldCloneSets)
        println(oldIdCloneMap)

        git.deleteRepo()
    }
}
