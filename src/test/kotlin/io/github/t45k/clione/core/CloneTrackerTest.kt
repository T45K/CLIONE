package io.github.t45k.clione.core

import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.controller.PullRequestController
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

internal class CloneTrackerTest {

    companion object {
        private const val REPOSITORY_FULL_NAME: String = "T45K/trial"
        private const val OLD_COMMIT_HASH = "f303aa58a4883781abec378ea435b524744c893c"
        private const val NEW_COMMIT_HASH = "49e312f1866d6d00432d6eed8f4904b65c8aba3e"
    }

    @Test
    fun test() {
        val config = RunningConfig("src", "java")
        val git = GitController.clone(REPOSITORY_FULL_NAME, "", 0)
        val pullRequest: PullRequestController = mockk<PullRequestController>()
            .also { every { it.getComparisonCommits() } returns (OLD_COMMIT_HASH to NEW_COMMIT_HASH) }
            .also { every { it.getRepositoryFullName() } returns REPOSITORY_FULL_NAME }
            .also { every { it.getNumber() } returns 0 }
        val cloneTracker = CloneTracker(git, pullRequest, config)
        val (oldCloneSets, newCloneSets) = cloneTracker.track()
    }
}
