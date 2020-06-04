package io.github.t45k.clione.controller

import io.github.t45k.clione.core.RunningConfig
import io.github.t45k.clione.entity.CloneInstance
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.IdCloneMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class NiCadControllerTest {

    companion object {
        private const val REPOSITORY_FULL_NAME: String = "T45K/trial"
        private const val OLD_COMMIT_HASH = "f303aa58a4883781abec378ea435b524744c893c"
    }

    @Test
    fun test() {
        val git: GitController = GitController.clone(REPOSITORY_FULL_NAME, "", 0)
        git.checkout(OLD_COMMIT_HASH)
        val config = RunningConfig("src", "java")
        val cloneDetector = NiCadController(git.getProjectPath().resolve(config.infix), config)

        val changedFiles: Set<String> = setOf("./storage/T45K/trial_0/src/Sample.java")
        val (cloneSets: List<Set<Int>>, idCloneMap: IdCloneMap) = cloneDetector.collectResult(changedFiles, CloneStatus.ADD)
        assertEquals(1, cloneSets.size)
        assertEquals(2, cloneSets[0].size)
        assertNotNull(idCloneMap[1])
        assertNotNull(idCloneMap[4])

        val clones: List<CloneInstance> = cloneDetector.parseCandidateXML()
        assertEquals(6, clones.size)

        git.deleteRepo()
    }
}
