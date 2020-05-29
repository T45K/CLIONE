package io.github.t45k.clione.controller

import org.kohsuke.github.GHPullRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PullRequestController(private val pullRequest: GHPullRequest) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun comment(message: String) {
        pullRequest.comment(message)
    }
}
