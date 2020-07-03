package io.github.t45k.clione.controller.cloneDetector

import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.controller.PullRequestController
import io.github.t45k.clione.core.Language
import io.github.t45k.clione.core.RunningConfig
import io.github.t45k.clione.entity.CloneInstance
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.IdCloneMap
import io.github.t45k.clione.util.generatePRMock
import io.github.t45k.clione.util.toRealPath
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class NiCadControllerTest {

    companion object {
        private const val REPOSITORY_FULL_NAME: String = "T45K/trial"
        private const val COMMIT_HASH = "f303aa58a4883781abec378ea435b524744c893c"
    }

    @Test
    fun test() {
        val pullRequest: PullRequestController = generatePRMock(REPOSITORY_FULL_NAME, 0, COMMIT_HASH)
        val git: GitController = GitController.cloneIfNotExists(REPOSITORY_FULL_NAME, "", pullRequest)
        val config = RunningConfig("src", Language.JAVA, similarity = 7)
        val cloneDetector = NiCadController(git.getProjectPath().resolve(config.src), config)

        val changedFiles: Set<Path> = setOf("./storage/T45K/trial_0/src/Sample.java".toRealPath())
        val fileCache: MutableMap<Path, List<String>> = mutableMapOf()
        val (cloneSets: List<Set<Int>>, idCloneMap: IdCloneMap) = cloneDetector.collectResult(changedFiles, CloneStatus.ADD, fileCache)
        assertEquals(1, cloneSets.size)
        assertEquals(2, cloneSets[0].size)
        assertNotNull(idCloneMap[1])
        assertNotNull(idCloneMap[4])

        val clones: List<CloneInstance> = cloneDetector.parseCandidateXML(fileCache, changedFiles)
        assertEquals(6, clones.size)

        git.close()
    }
}
