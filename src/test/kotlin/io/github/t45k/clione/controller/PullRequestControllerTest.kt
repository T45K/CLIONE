package io.github.t45k.clione.controller

import org.kohsuke.github.GitHubBuilder
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PullRequestControllerTest {
    private lateinit var pullRequestController: PullRequestController

    @BeforeTest
    fun setUp() {
        pullRequestController = GitHubBuilder.fromEnvironment()
            .build()
            .getRepository("T45K/CLIONE")
            .getPullRequest(1)
            .run { PullRequestController(this) }
    }

    @Test
    fun testGetComparisonCommits() {
        GitController.cloneIfNotExists("T45K/CLIONE", "", pullRequestController).use { git ->
            val (oldCommitHash: String, newCommitHash: String) = pullRequestController.getComparisonCommits(git)
            assertEquals("6b177f614af8d904df6852150bec0d0af36d3f5a", oldCommitHash)
            assertEquals("e10bdde6df7d804dc9d23c24d6d2b9ea716adc09", newCommitHash)
        }
    }
}
