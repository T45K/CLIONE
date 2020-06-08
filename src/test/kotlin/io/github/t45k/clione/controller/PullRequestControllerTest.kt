package io.github.t45k.clione.controller

import org.kohsuke.github.GitHubBuilder
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PullRequestControllerTest {
    lateinit var pullRequestController: PullRequestController

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
        val (oldCommitHash: String, newCommitHash: String) = pullRequestController.getComparisonCommits()
        assertEquals("6b177f614af8d904df6852150bec0d0af36d3f5a", oldCommitHash)
        assertEquals("5d758afe17613427350346ec453fd1804c942f74", newCommitHash)
    }
}
