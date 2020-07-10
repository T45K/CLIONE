import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.controller.PullRequestController
import io.github.t45k.clione.core.CloneTracker
import io.github.t45k.clione.core.RunningConfig
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GitHubBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test

class Exp1 {

    @Test
    fun investigateMaintenanceTargetClonesProportion() {
        val repositoryFullName = "jfree/jfreechart"
        var javaFileChangedPRCount = 0
        var targetClonesPRCount = 0
        val prInfos = mutableListOf<String>()
        GitHubBuilder.fromEnvironment()
            //.withPassword("T45K","")
            .build()
            .getRepository(repositoryFullName)
            .getPullRequests(GHIssueState.CLOSED)
            .filter { it.isMerged }
            .forEach { ghPR ->
                val pullRequest = PullRequestController(ghPR)
                try {
                    GitController.cloneIfNotExists(repositoryFullName, "", pullRequest).use { git ->
                        val (tmp, head) = pullRequest.getComparisonCommits()
                        val base = git.getCommonAncestorCommit(tmp, head)
                        val (oldChangedFiles, newChangedFiles) = git.findChangedFiles(base, head)
                        val config = if (Files.exists(git.getProjectPath().resolve("src"))) {
                            RunningConfig("src/main/java")
                        } else {
                            RunningConfig("source")
                        }
                        val isJavaFileChanged = setOf(*oldChangedFiles.toTypedArray(), *newChangedFiles.toTypedArray())
                            .any { it.toString().contains(config.src) && it.toString().endsWith(".java") }
                        if (!isJavaFileChanged) {
                            return@use
                        }

                        javaFileChangedPRCount++
                        val tracker = CloneTracker(git, pullRequest, config)
                        val (oldClones, newClones) = tracker.track()
                        if (oldClones.isEmpty() && newClones.isEmpty()) {
                            return@use
                        }

                        targetClonesPRCount++
                        prInfos.add("${pullRequest.number} $base $head")

                    }
                } catch (e: Exception) {
                    // do nothing
                }
            }

        prInfos.add("all: $javaFileChangedPRCount")
        prInfos.add("target: $targetClonesPRCount")
        Files.writeString(Path.of(repositoryFullName.replace("/", "_")), prInfos.joinToString("\n"))
    }
}