package io.github.t45k.clione.controller

import io.github.t45k.clione.core.TrackingResult
import io.github.t45k.clione.entity.CloneInstance
import io.github.t45k.clione.entity.CloneStatus
import org.kohsuke.github.GHCheckRun
import org.kohsuke.github.GHCheckRunBuilder
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHPullRequestFileDetail
import org.kohsuke.github.GHPullRequestReviewEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.math.min

class PullRequestController(private val pullRequest: GHPullRequest) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private const val APP_NAME = "CLIONE"
    }

    private val changedFiles: Map<String, List<GHPullRequestFileDetail>> =
        pullRequest.listFiles().toList().groupBy { it.filename }
    val headCommitHash: String = pullRequest.head.sha
    val number: Int = pullRequest.number
    val fullName: String = pullRequest.repository.fullName

    /**
     * Comment to the Pull Request to notify inconsistent changes of clone sets.
     * Notice:
     */
    fun comment(trackingResult: TrackingResult) {
        logger.info("[START]\tComment about $fullName/$number")

        if (trackingResult === TrackingResult.EMPTY) {
            pullRequest.comment("No problem. Good job! ")
        }

        for (inconsistentlyChangedClones in trackingResult.inconsistentlyChangedCloneSets) {
            createCommentAboutInconsistentlyChangedClones(inconsistentlyChangedClones)
        }

        for (newCloneAddedCloneSet in trackingResult.newCloneAddedCloneSets) {
            createCommentAboutNewCloneAddedCloneSet(newCloneAddedCloneSet)
        }

        for (newlyCreatedCloneSet in trackingResult.newlyCreatedCloneSets) {
            createCommentAboutNewlyCreatedCloneSet(newlyCreatedCloneSet)
        }

        for (unmergedCloneSet in trackingResult.unmergedCloneSets) {
            // TODO impl
        }

        logger.info("[END]\tComment about $fullName/${pullRequest.number}")
    }

    private fun createCommentAboutInconsistentlyChangedClones(cloneSet: List<CloneInstance>) {
        val changedClone = cloneSet.find { it.status == CloneStatus.MODIFY }
            ?: return

        val otherClones: List<CloneInstance> = cloneSet.filter { it.status != CloneStatus.MODIFY }

        val form = if (otherClones.size == 1) {
            "fragment is"
        } else {
            "fragments are"
        }

        val src: Regex = "storage/[^/]+/[^/]+/".toRegex()
        val fileName = src.split(changedClone.filePath.toString())[1]
        val detail: GHPullRequestFileDetail = (changedFiles[fileName] ?: error(""))[0]

        val multiLine = calculateMultiLines(changedClone, detail)

        val clonePlaces: String =
            otherClones.joinToString("\n") { "${getFileUrlBase()}/${src.split(it.filePath.toString())[1]}#L${it.startLine}-L${it.endLine}" }

        val body =
            """In this Pull Request, this code fragment in ${getFileUrlBase()}/$fileName#L${changedClone.startLine}-L${changedClone.endLine} is modified,
                |but the following $form unmodified.
                |$clonePlaces
                |
                |Why don't you modify consistently?""".trimMargin()

        logger.info("Comment is\n$body")

        pullRequest.createMultiLineReviewComment(
            body, pullRequest.head.sha,
            fileName, "RIGHT", "RIGHT", multiLine[0].first, multiLine[0].second
        )
    }

    private fun createCommentAboutNewlyCreatedCloneSet(cloneSet: List<CloneInstance>) {
        val src: Regex = "storage/[^/]+/[^/]+/".toRegex()
        val clonePlaces: String =
            cloneSet.joinToString("\n") { "${getFileUrlBase()}/${src.split(it.filePath.toString())[1]}#L${it.startLine}-L${it.endLine}" }

        val body = """In this Pull Request, these clone sets are created.
                |
                |$clonePlaces
                |
                |Why don't you merge it?""".trimMargin()

        logger.info("Comment is\n$body")

        pullRequest.createReview()
            .body(body)
            .event(GHPullRequestReviewEvent.COMMENT)
            .commitId(pullRequest.head.sha)
    }

    private fun createCommentAboutNewCloneAddedCloneSet(cloneSet: List<CloneInstance>) {
        val addedClone = cloneSet.find { it.status == CloneStatus.ADD }
            ?: return

        val otherClones: List<CloneInstance> = cloneSet.filter { it != addedClone }

        val src: Regex = "storage/[^/]+/[^/]+/".toRegex()
        val fileName = src.split(addedClone.filePath.toString())[1]
        val detail: GHPullRequestFileDetail = (changedFiles[fileName] ?: error(""))[0]

        val multiLine = calculateMultiLines(addedClone, detail)
        val clonePlaces: String =
            otherClones.joinToString("\n") { "${getFileUrlBase()}/${src.split(it.filePath.toString())[1]}#L${it.startLine}-L${it.endLine}" }

        val body =
            """In this Pull Request, this code fragment in In this Pull Request, above code fragment is added to below clone set, 
                |
                |$clonePlaces
                |
                |Why don't you merge it?""".trimMargin()

        logger.info("Comment is\n$body")

        pullRequest.createMultiLineReviewComment(
            body, pullRequest.head.sha,
            fileName, "RIGHT", "RIGHT", multiLine[0].first, multiLine[0].second
        )
    }

    private fun calculateMultiLines(clone: CloneInstance, detail: GHPullRequestFileDetail): List<Pair<Int, Int>> =
        "\\+([0-9]+,[0-9]+)\\s".toRegex().findAll(detail.patch)
            .map { it.value }
            .map { it.substring(1, it.length - 1) }
            .map { it.split(",") }
            .map { it[0].toInt() to (it[0].toInt() + it[1].toInt() - 1) }
            .filterNot { it.second < clone.startLine || clone.endLine < it.first }
            .map { max(it.first, clone.startLine) to min(it.second, clone.endLine) }
            .toList()

    fun errorComment() {
        pullRequest.comment("Oops! Something is matter.")
    }

    /**
     * Return base commit hash and head of the branch of the pull request.
     */
    fun getComparisonCommits(git: GitController): Pair<String, String> =
        if (pullRequest.isMerged) {
            git.getParentCommit(pullRequest.mergeCommitSha) to pullRequest.mergeCommitSha
        } else {
            git.getCommonAncestorCommit(pullRequest.base.sha, pullRequest.head.sha) to pullRequest.head.sha
        }

    /**
     * Return GitHub Url of the pull request file base
     * example: https://github.com/T45k/Clione/blob/master/
     */
    private fun getFileUrlBase(): String =
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

    @Suppress("DEPRECATION")
    fun sendInProgressStatus() =
        pullRequest.repository.createCheckRun(APP_NAME, headCommitHash)
            .withStatus(GHCheckRun.Status.IN_PROGRESS)
            .create()

    @Suppress("DEPRECATION")
    fun sendSuccessStatus(successMessage: String) =
        pullRequest.repository.createCheckRun(APP_NAME, headCommitHash)
            .withStatus(GHCheckRun.Status.COMPLETED)
            .withConclusion(GHCheckRun.Conclusion.SUCCESS)
            .add(GHCheckRunBuilder.Output("Success", successMessage))
            .add(GHCheckRunBuilder.Action("rerun", "rerun CLIONE", "rerun"))
            .create()

    @Suppress("DEPRECATION")
    fun sendErrorStatus(errorMessage: String) =
        pullRequest.repository.createCheckRun(APP_NAME, headCommitHash)
            .withStatus(GHCheckRun.Status.COMPLETED)
            .withConclusion(GHCheckRun.Conclusion.CANCELLED)
            .add(GHCheckRunBuilder.Output("Error", errorMessage))
            .add(GHCheckRunBuilder.Action("rerun", "rerun CLIONE", "rerun"))
            .create()
}
