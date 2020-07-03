package io.github.t45k.clione.controller

import io.github.t45k.clione.entity.FileChangeType
import io.github.t45k.clione.util.generatePRMock
import org.kohsuke.github.GitHubBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class GitControllerTest {

    companion object {
        private const val REPOSITORY_FULL_NAME: String = "T45K/trial"
        private const val OLD_COMMIT_HASH = "b0ba3a8435f30f0ed96ac293597378c42d795c55"
        private const val NEW_COMMIT_HASH = "646853e1ddabab861847eca04e72cccfc76963e3"
    }

    private val pullRequest: PullRequestController = generatePRMock(REPOSITORY_FULL_NAME, 0, NEW_COMMIT_HASH)
    private val git: GitController = GitController.cloneIfNotExists(REPOSITORY_FULL_NAME, "", pullRequest)

    @Test
    fun testCloneSuccess() {
        assertTrue(Files.exists(Path.of("storage", "${REPOSITORY_FULL_NAME}_0")))
    }

    @Test
    fun testFindChangeFiles() {
        val (oldChangedFiles: Set<Path>, newChangedFiles: Set<Path>) = git.findChangedFiles(OLD_COMMIT_HASH, NEW_COMMIT_HASH)
        assertEquals(3, oldChangedFiles.size)
        assertTrue(oldChangedFiles.contains(git.getProjectPath().resolve("src/Sample.java").toRealPath()))
        assertEquals(3, newChangedFiles.size)
        assertTrue(newChangedFiles.contains(git.getProjectPath().resolve("src/Sample.java").toRealPath()))
    }

    @Test
    fun testFindMergeBasedChangedFiles() {
        val pullRequest: PullRequestController = GitHubBuilder.fromEnvironment()
            .build()
            .getRepository("alibaba/fastjson")
            .getPullRequest(3268)
            .run { PullRequestController(this) }
        val git: GitController = GitController.cloneIfNotExists("alibaba/fastjson", "", pullRequest)
        val changedFiles: Pair<Set<Path>, Set<Path>> = git.findChangedFiles(
            pullRequest.getComparisonCommits().first,
            pullRequest.getComparisonCommits().second
        )

        assertEquals(2, changedFiles.first.size)
        assertEquals(2, changedFiles.second.size)

        git.close()
    }

    @Test
    fun testCalcFileDiff() {
        val filePath = git.getProjectPath().resolve("src/Sample.java").toRealPath()
        val (type, lineMapping, newFileName) = git.calcFileDiff(filePath, OLD_COMMIT_HASH, NEW_COMMIT_HASH)
        assertEquals(FileChangeType.MODIFY, type)
        lineMapping.forEach { assertEquals(0, it) }
        assertEquals(filePath, newFileName)
    }

    @AfterTest
    fun deleteRepo() {
        git.close()
    }
}
