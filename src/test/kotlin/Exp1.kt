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
        val repositoryFullName = "dnsjava/dnsjava"
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
                        val isJavaFileChanged = setOf(*oldChangedFiles.toTypedArray(), *newChangedFiles.toTypedArray())
                            .any { it.toString().contains("src/main/java") && it.toString().endsWith(".java") }
                        if (!isJavaFileChanged) {
                            return@forEach
                        }

                        javaFileChangedPRCount++
                        val config = RunningConfig("src/main/java")
                        val tracker = CloneTracker(git, pullRequest, config)
                        val (oldClones, newClones) = tracker.track()
                        if (oldClones.isEmpty() && newClones.isEmpty()) {
                            return@forEach
                        }

                        targetClonesPRCount++
                        prInfos.add("${pullRequest.number} $base $head")
                    }
                } catch (e: Exception) {
                    println(ghPR.number)
                }
            }

        Files.writeString(Path.of("result"), prInfos.joinToString("\n"))
    }
}