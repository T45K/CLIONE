package io.github.t45k.clione.controller

import org.kohsuke.github.GHPullRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PullRequestController(private val pullRequest: GHPullRequest) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    fun comment(message: String) {
        pullRequest.comment(message)
    }

    /**
     * Return base commit hash and head of the branch of the pull request.
     */
    fun getComparisonCommits(): Pair<String, String> = pullRequest.base.sha to pullRequest.head.sha

    /**
     * Return GitHub Url of the pull request file base
     * example: https://github.com/T45k/Clione/blob/master/
     */
    fun getFileUrlBase(): String =
        "https://github.com/${pullRequest.head.repository.fullName}/blob/${getBranchName()}"

    /**
     * Whether the pull request was opened from forked repository
     */
    private fun isPullRequestWithFork(): Boolean = pullRequest.head.label.contains(":")

    private fun getBranchName(): String =
        if (isPullRequestWithFork()) {
            pullRequest.head.label.substringAfter(":")
        } else {
            pullRequest.head.label
        }

    fun getRepositoryFullName(): String = pullRequest.repository.fullName
    fun getNumber(): Int = pullRequest.number
}
