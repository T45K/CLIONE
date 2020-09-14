package io.github.t45k.clione

import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.controller.PullRequestController
import io.github.t45k.clione.core.CloneTracker
import io.github.t45k.clione.core.config.ConfigGeneratorFactory
import io.github.t45k.clione.core.config.RunningConfig
import io.github.t45k.clione.entity.InvalidConfigSpecifiedException
import org.eclipse.jgit.errors.MissingObjectException
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHubBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.ResourceBundle

class StandAloneEntryPoint {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
}

fun main(args: Array<String>) {
    val entryPoint = StandAloneEntryPoint()
    if (args.isEmpty()) {
        entryPoint.logger.error("Specify repository full name and source file")
        return
    }

    val repositoryFullName: String = args[0]
    val propertyName: String = if (args.size > 1) args[1] else "stand_alone"
    val (userName: String, oAuthToken: String) = ResourceBundle.getBundle(propertyName)
        ?.let { (it.getString("USER_NAME") ?: "") to (it.getString("O_AUTH_TOKEN") ?: "") }
        ?: throw InvalidConfigSpecifiedException("$propertyName.properties was not found")
    val config: RunningConfig = ConfigGeneratorFactory.fromProperties(propertyName)

    val repository: GHRepository = GitHubBuilder.fromEnvironment()
        .apply {
            if (userName.isNotEmpty() && oAuthToken.isNotEmpty()) {
                this.withOAuthToken(oAuthToken, userName)
            }
        }
        .build()
        .getRepository(repositoryFullName)

    val mainHead: String = repository.listCommits().first().shA1

    val git: GitController =
        GitController.cloneIfNotExists(repositoryFullName, userName, oAuthToken, repository.defaultBranch)
    git.checkout(mainHead)

    repository.getPullRequests(GHIssueState.CLOSED).asSequence()
        .filter { it.isMerged }
        .filter { pr ->
            entryPoint.logger.info("enter PR#${pr.number}")
            try {
                val mergeCommit: String = pr.mergeCommitSha
                val parentCommit: String = git.getParentCommit(mergeCommit)
                git.findChangedFiles(parentCommit, mergeCommit)
                    .let { setOf(*it.first.toTypedArray(), *it.second.toTypedArray()) }
                    .any { it.toString().contains(config.src) && it.toString().endsWith(config.lang.extension) }
            } catch (e: MissingObjectException) {
                entryPoint.logger.error(e.toString())
                false
            }
        }
        .onEach { git.checkout(mainHead) }
        .map {
            val pullRequest = PullRequestController(it)
            it.number to CloneTracker(git, pullRequest, config).track().getRaw()
        }
        .filter { it.second.first.isNotEmpty() || it.second.second.isNotEmpty() }
        .joinToString("\n\n") { (prNumber, pair) ->
            """$prNumber
                    |
                    |new:
                    |${pair.second.joinToString("\n\n") { it.joinToString("\n") }}
                    |
                    |old:
                    |${pair.first.joinToString("\n\n") { it.joinToString("\n") }}
                """.trimMargin()
        }
        .let { Files.writeString(Path.of("${repositoryFullName.replace("/", "_")}_result"), it) }
}
