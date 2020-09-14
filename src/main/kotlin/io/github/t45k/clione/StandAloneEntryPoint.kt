package io.github.t45k.clione

import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.controller.PullRequestController
import io.github.t45k.clione.core.CloneTracker
import io.github.t45k.clione.core.config.ConfigGeneratorFactory
import io.github.t45k.clione.core.config.RunningConfig
import io.github.t45k.clione.entity.InvalidConfigSpecifiedException
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHubBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.ResourceBundle

class StandAloneEntryPoint(args: Array<String>) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val repositoryFullName: String
    private val config: RunningConfig
    private val repository: GHRepository
    private val mainHead: String
    private val git: GitController

    init {
        if (args.isEmpty()) {
            throw InvalidConfigSpecifiedException("Specify repository full name and source file")
        }

        repositoryFullName = args[0]
        val propertyName: String = if (args.size > 1) args[1] else "stand_alone"
        val (userName: String, oAuthToken: String) = ResourceBundle.getBundle(propertyName)
            ?.let { (it.getString("USER_NAME") ?: "") to (it.getString("O_AUTH_TOKEN") ?: "") }
            ?: throw InvalidConfigSpecifiedException("$propertyName.properties was not found")
        config = ConfigGeneratorFactory.fromProperties(propertyName)

        repository = GitHubBuilder.fromEnvironment()
            .apply {
                if (userName.isNotEmpty() && oAuthToken.isNotEmpty()) {
                    this.withOAuthToken(oAuthToken, userName)
                }
            }
            .build()
            .getRepository(repositoryFullName)
        mainHead = repository.listCommits().first().shA1
        git = GitController.cloneIfNotExists(repositoryFullName, userName, oAuthToken, repository.defaultBranch)
    }

    fun run() {
        repository.getPullRequests(GHIssueState.CLOSED).asSequence()
            .filter { it.isMerged }
            .filter { pr: GHPullRequest ->
                logger.info("enter PR#${pr.number}")
                try {
                    val mergeCommit: String = pr.mergeCommitSha
                    val parentCommit: String = git.getParentCommit(mergeCommit)
                    git.findChangedFiles(parentCommit, mergeCommit)
                        .let { setOf(*it.first.toTypedArray(), *it.second.toTypedArray()) }
                        .any { it.toString().contains(config.src) && it.toString().endsWith(config.lang.extension) }
                } catch (e: Exception) {
                    logger.error(e.toString())
                    false
                }
            }
            .onEach { git.checkout(mainHead) }
            .map {
                val pullRequest = PullRequestController(it)
                it.number to CloneTracker(git, pullRequest, config).track().getRaw()
            }
            .filter { it.second.first.isNotEmpty() || it.second.second.isNotEmpty() }
            .map { (prNumber, pair) ->
                """$prNumber
                        |
                        |new:
                        |${pair.second.joinToString("\n\n") { it.joinToString("\n") }}
                        |
                        |old:
                        |${pair.first.joinToString("\n\n") { it.joinToString("\n") }}
                        |
                    """.trimMargin()
            }
            .forEach {
                Files.writeString(
                    Path.of("${repositoryFullName.replace("/", "_")}_result"),
                    it,
                    StandardOpenOption.APPEND, StandardOpenOption.WRITE
                )
            }
    }
}

fun main(args: Array<String>) {
    StandAloneEntryPoint(args).run()
}

