package io.github.t45k.clione

import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.controller.PullRequestController
import io.github.t45k.clione.core.CloneTracker
import io.github.t45k.clione.core.config.ConfigGeneratorFactory
import io.github.t45k.clione.core.config.RunningConfig
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

    val property: ResourceBundle = ResourceBundle.getBundle(propertyName) ?: return
    val (userName: String, oAuthToken: String) = property
        .let { (it.getString("USER_NAME") ?: "") to (it.getString("O_AUTH_TOKEN") ?: "") }
    val config: RunningConfig = ConfigGeneratorFactory.fromProperties(propertyName)

    val repository: GHRepository = GitHubBuilder.fromEnvironment()
        .withOAuthToken(oAuthToken, userName)
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
                val headCommit: String = pr.head.sha
                val baseCommit: String = git.getCommonAncestorCommit(pr.base.sha, headCommit)
                git.findChangedFiles(baseCommit, headCommit)
                    .let { setOf(*it.first.toTypedArray(), *it.second.toTypedArray()) }
                    .any { it.toString().contains(config.src) && it.toString().endsWith(config.lang.extension) }
            } catch (e: MissingObjectException) {
                false
            }
        }
        .onEach { git.checkout(mainHead) }
        .joinToString("\n\n") { pr ->
            val pullRequest = PullRequestController(pr)
            val (oldCloneSets, newCloneSets) = CloneTracker(git, pullRequest, config).track().getRaw()
            """${pr.number}
                    |
                    |new:
                    |${newCloneSets.joinToString("\n\n") { it.joinToString("\n") }}
                    |
                    |old:
                    |${oldCloneSets.joinToString("\n\n") { it.joinToString("\n") }}
                """.trimMargin()
        }
        .let { Files.writeString(Path.of("${repositoryFullName.replace("/", "_")}_result"), it) }
}
