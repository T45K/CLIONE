package io.github.t45k.clione.controller.cloneDetector.cloneCandidate

import io.github.t45k.clione.controller.cloneDetector.cloneCandidate.kotlin.KotlinBlockExtractor
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.util.EMPTY_NAME_PATH
import kotlin.test.Test
import kotlin.test.assertEquals

internal class KotlinBlockExtractorTest {

    @Test
    fun test() {
        val code =
            """package io.github.t45k.clione.controller

import io.github.t45k.clione.entity.CloneInstance
import io.github.t45k.clione.entity.CloneStatus
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHPullRequestFileDetail
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.math.min

class PullRequestController(private val pullRequest: GHPullRequest) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val changedFiles: Map<String, List<GHPullRequestFileDetail>> = pullRequest.listFiles().toList().groupBy { it.filename }
    val headCommitHash: String = pullRequest.head.sha

    /**
     * Comment to the Pull Request to notify inconsistent changes of clone sets.
     * Notice:
     */
    fun comment(inconsistentChangedCloneSets: List<List<CloneInstance>>) {
        logger.info("[START]\tComment about")
        for (newInconsistentChangedClones in inconsistentChangedCloneSets) {
            val inconsistentChangedClone = newInconsistentChangedClones.find { it.status == CloneStatus.MODIFY }
                ?: continue

            val stableClones: List<CloneInstance> = newInconsistentChangedClones.filter { it.status == CloneStatus.STABLE }

            val form = if (stableClones.size == 1) {
                "fragment is"
            } else {
                "fragments are"
            }

            val infix: Regex = "storage/[^/]+/[^/]+/".toRegex()
            val fileName = infix.split(inconsistentChangedClone.fileName)[1]
            val detail: GHPullRequestFileDetail = (changedFiles[fileName] ?: error(""))[0]

            val multiLine = "\\+([0-9]+,[0-9]+)\\s".toRegex().findAll(detail.patch)
                .map { it.value }
                .map { it.substring(1, it.length - 1) }
                .map { it.split(",") }
                .map { it[0].toInt() to (it[0].toInt() + it[1].toInt() - 1) }
                .filterNot { it.second < inconsistentChangedClone.startLine || inconsistentChangedClone.endLine < it.first }
                .map { max(it.first, inconsistentChangedClone.startLine) to min(it.second, inconsistentChangedClone.endLine) }
                .toList()

            val clonePlaces: String = stableClones.joinToString("\n") { " { } " }

            val body = ""${'"'}In this Pull Request, this code fragment in 
            |
            |
            |
            |""${'"'}.trimMargin()

            logger.info("Comment is\n")

            pullRequest.createMultiLineReviewComment(body, pullRequest.head.sha,
                fileName, multiLine[0].first, multiLine[0].second)
            println()
        }
        logger.info("[END]\tComment about {  } ")
    }

    fun errorComment() {
        pullRequest.comment("Oops! Something is matter.")
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
        "https://github.com/"

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
"""
        val blocks: List<Pair<LazyCloneInstance, String>> = KotlinBlockExtractor().extract(code, EMPTY_NAME_PATH, CloneStatus.STABLE)

        // fun comment and for stmt
        assertEquals(2, blocks.size)
    }
}