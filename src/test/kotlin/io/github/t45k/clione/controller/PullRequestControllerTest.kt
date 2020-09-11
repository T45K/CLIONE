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
        // This PR was merged.
        // Thus, oldCommitHash returns empty string.
        val (oldCommitHash: String, newCommitHash: String) = pullRequestController.getComparisonCommits()
        assertEquals("", oldCommitHash)
        assertEquals("e10bdde6df7d804dc9d23c24d6d2b9ea716adc09", newCommitHash)
    }
}
