@file:Suppress("DEPRECATION")

package io.github.t45k.clione.controller

import io.github.t45k.clione.entity.CloneInstance
import io.github.t45k.clione.entity.CloneStatus
import org.kohsuke.github.GHCheckRun
import org.kohsuke.github.GHCheckRunBuilder
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHPullRequestFileDetail
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.math.min

class PullRequestController(private val pullRequest: GHPullRequest) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private const val APP_NAME = "CLIONE"
    }

    private val changedFiles: Map<String, List<GHPullRequestFileDetail>> = pullRequest.listFiles().toList().groupBy { it.filename }
    val headCommitHash: String = pullRequest.head.sha
    val number: Int = pullRequest.number
    val fullName: String = pullRequest.repository.fullName

    /**
     * Comment to the Pull Request to notify inconsistent changes of clone sets.
     * Notice:
     */
    fun comment(inconsistentChangedCloneSets: List<List<CloneInstance>>) {
        logger.info("[START]\tComment about $fullName/$number")

        if (inconsistentChangedCloneSets.isEmpty()) {
            pullRequest.comment("Neither inconsistent changed nor new clone sets were detected.\nGood job! ")
        }

        for (inconsistentChangedClones in inconsistentChangedCloneSets) {
            val inconsistentChangedClone = inconsistentChangedClones.find { it.status == CloneStatus.MODIFY }
                ?: continue

            val stableClones: List<CloneInstance> = inconsistentChangedClones.filter { it.status == CloneStatus.STABLE }

            val form = if (stableClones.size == 1) {
                "fragment is"
            } else {
                "fragments are"
            }

            val src: Regex = "storage/[^/]+/[^/]+/".toRegex()
            val fileName = src.split(inconsistentChangedClone.filePath.toString())[1]
            val detail: GHPullRequestFileDetail = (changedFiles[fileName] ?: error(""))[0]

            val multiLine = "\\+([0-9]+,[0-9]+)\\s".toRegex().findAll(detail.patch)
                .map { it.value }
                .map { it.substring(1, it.length - 1) }
                .map { it.split(",") }
                .map { it[0].toInt() to (it[0].toInt() + it[1].toInt() - 1) }
                .filterNot { it.second < inconsistentChangedClone.startLine || inconsistentChangedClone.endLine < it.first }
                .map { max(it.first, inconsistentChangedClone.startLine) to min(it.second, inconsistentChangedClone.endLine) }
                .toList()

            val clonePlaces: String = stableClones.joinToString("\n") { "${getFileUrlBase()}/${src.split(it.filePath.toString())[1]}#L${it.startLine}-L${it.endLine}" }

            val body = """In this Pull Request, this code fragment in ${getFileUrlBase()}/$fileName#L${inconsistentChangedClone.startLine}-L${inconsistentChangedClone.endLine} is modified,
                |but the following $form unmodified.
                |$clonePlaces
                |
                |Why don't you modify consistently?""".trimMargin()

            logger.info("Comment is\n$body")

            pullRequest.createMultiLineReviewComment(body, pullRequest.head.sha,
                fileName, multiLine[0].first, multiLine[0].second)
            println()
        }
        logger.info("[END]\tComment about $fullName/${pullRequest.number}")
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
        "https://github.com/${pullRequest.head.repository.fullName}/blob/${pullRequest.head.sha}"

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

    fun sendInProgressStatus() =
        pullRequest.repository.createCheckRun(APP_NAME, headCommitHash)
            .withStatus(GHCheckRun.Status.IN_PROGRESS)
            .create()

    fun sendSuccessStatus() =
        pullRequest.repository.createCheckRun(APP_NAME, headCommitHash)
            .withStatus(GHCheckRun.Status.COMPLETED)
            .withConclusion(GHCheckRun.Conclusion.SUCCESS)
            .add(GHCheckRunBuilder.Output("Success", "Notification is completed."))
            .add(GHCheckRunBuilder.Action("rerun", "rerun CLIONE", "rerun"))
            .create()

    fun sendErrorStatus(errorMessage: String) =
        pullRequest.repository.createCheckRun(APP_NAME, headCommitHash)
            .withStatus(GHCheckRun.Status.COMPLETED)
            .withConclusion(GHCheckRun.Conclusion.CANCELLED)
            .add(GHCheckRunBuilder.Output("Error", errorMessage))
            .add(GHCheckRunBuilder.Action("rerun", "rerun CLIONE", "rerun"))
            .create()
}
