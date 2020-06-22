package io.github.t45k.clione.controller.cloneDetector

import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.core.Language
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
        val git: GitController = GitController.clone(REPOSITORY_FULL_NAME, "", 0, OLD_COMMIT_HASH)
        val config = RunningConfig("src", Language.JAVA)
        val cloneDetector = NiCadController(git.getProjectPath().resolve(config.infix), config)

        val changedFiles: Set<String> = setOf("./storage/T45K/trial_0/src/Sample.java")
        val fileCache: MutableMap<String, List<String>> = mutableMapOf()
        val (cloneSets: List<Set<Int>>, idCloneMap: IdCloneMap) = cloneDetector.collectResult(changedFiles, CloneStatus.ADD, fileCache)
        assertEquals(1, cloneSets.size)
        assertEquals(2, cloneSets[0].size)
        assertNotNull(idCloneMap[1])
        assertNotNull(idCloneMap[4])

        val clones: List<CloneInstance> = cloneDetector.parseCandidateXML(fileCache, changedFiles)
        assertEquals(6, clones.size)

        git.deleteRepo()
    }
}
