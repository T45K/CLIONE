package io.github.t45k.clione.controller

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class GitControllerTest {

    @Test
    fun testCloneAndDelete() {
        val repositoryFullName = "danilofes/refactoring-toy-example"
        val git: GitController = GitController.clone(repositoryFullName, "", 0)
        assertTrue(Files.exists(Path.of("storage", "${repositoryFullName}_0")))

        git.deleteRepo()
        assertFalse(Files.exists(Path.of("storage", "${repositoryFullName}_0")))
    }
}