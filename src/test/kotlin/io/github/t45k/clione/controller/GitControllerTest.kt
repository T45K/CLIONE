package io.github.t45k.clione.controller

import io.github.t45k.clione.entity.FileChangeType
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

    private val git: GitController = GitController.clone(REPOSITORY_FULL_NAME, "", 0, NEW_COMMIT_HASH)

    @Test
    fun testCloneSuccess() {
        assertTrue(Files.exists(Path.of("storage", "${REPOSITORY_FULL_NAME}_0")))
    }

    @Test
    fun testFindChangeFiles() {
        val (oldChangedFiles: Set<String>, newChangedFiles: Set<String>) = git.findChangedFiles(OLD_COMMIT_HASH, NEW_COMMIT_HASH)
        assertEquals(3, oldChangedFiles.size)
        assertTrue(oldChangedFiles.contains("${git.getProjectPath()}/src/Sample.java"))
        assertEquals(3, newChangedFiles.size)
        assertTrue(newChangedFiles.contains("${git.getProjectPath()}/src/Sample.java"))
    }

    @Test
    fun testCalcFileDiff() {
        val filePath = "${git.getProjectPath()}/src/Sample.java"
        val (type, lineMapping, newFileName) = git.calcFileDiff(filePath, OLD_COMMIT_HASH, NEW_COMMIT_HASH)
        assertEquals(FileChangeType.MODIFY, type)
        lineMapping.forEach { assertEquals(0, it) }
        assertEquals(filePath, newFileName)
    }

    @AfterTest
    fun deleteRepo() {
        git.deleteRepo()
    }
}
