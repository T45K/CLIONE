package io.github.t45k.clione.util

import io.github.t45k.clione.controller.PullRequestController
import io.mockk.every
import io.mockk.mockk

fun generatePRMock(repositoryFullName: String, number: Int, headSha: String, baseSha: String = "") =
    mockk<PullRequestController>()
        .also { every { it.fullName } returns repositoryFullName }
        .also { every { it.number } returns number }
        .also { every { it.headCommitHash } returns headSha }
        .also { every { it.getComparisonCommits() } returns (baseSha to headSha) }