package io.github.t45k.clione

import com.github.kusumotolab.sdl4j.util.CommandLine
import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.controller.PullRequestController
import io.github.t45k.clione.core.CloneTracker
import io.github.t45k.clione.core.config.ConfigGeneratorFactory
import io.github.t45k.clione.core.config.RunningConfig
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.InvalidConfigSpecifiedException
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHubBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
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

    private fun fetchMergeCommitByParsingHTML(prNumber: Int): String =
        URL("https://github.com/$repositoryFullName/pull/$prNumber").openConnection().getInputStream()
            .let(::InputStreamReader)
            .let(::BufferedReader)
            .lineSequence()
            .first { it.trim().startsWith("merged commit") }
            .substringAfter("commit/").substring(0, 40)

    fun run() {
        var targetCount = 0
        repository.getPullRequests(GHIssueState.CLOSED).asSequence()
            .filter { it.isMerged }
            .filter { pr: GHPullRequest ->
                logger.info("enter PR#${pr.number}")
                try {
                    val mergeCommit: String = fetchMergeCommitByParsingHTML(pr.number)
                    val parentCommit: String = git.getParentCommit(mergeCommit)
                    git.findChangedFiles(parentCommit, mergeCommit)
                        .let { setOf(*it.first.toTypedArray(), *it.second.toTypedArray()) }
                        .any { it.toString().contains(config.src) && it.toString().endsWith(config.lang.extension) }
                } catch (e: Exception) {
                    logger.error(e.toString())
                    false
                }
            }
            .onEach { targetCount++ }
            .onEach { //git.checkout(mainHead)
                CommandLine().execute(File("./storage/$repositoryFullName"), "git", "checkout", mainHead)
            }
            .map {
                val pullRequest = PullRequestController(it)
                it.number to CloneTracker(git, pullRequest, config).track().getRaw()
            }
            .filterNot { it.second.first.isEmpty() }
            .map { (prNumber, pair) ->
                val old = pair.first
                val stables: Int = old.filter { cloneSet -> cloneSet.all { it.status == CloneStatus.MODIFY } }.count()
                val inconsistent: Int =
                    old.filter { cloneSet ->
                        cloneSet.any { it.status == CloneStatus.STABLE }
                            && cloneSet.any { it.status == CloneStatus.MODIFY }
                    }
                        .count()
                "$prNumber\n$stables\n$inconsistent\n${old.joinToString("\n\n") { it.joinToString("\n") }}\n\n"
            }
            .forEach {
                Files.writeString(
                    Path.of("${repositoryFullName.replace("/", "_")}_result"),
                    it,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
                )
            }
        println(targetCount)
    }
}

fun main(args: Array<String>) {
    StandAloneEntryPoint(args).run()
}

