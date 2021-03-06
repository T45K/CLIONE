package io.github.t45k.clione.core

import com.google.common.annotations.VisibleForTesting
import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.controller.PullRequestController
import io.github.t45k.clione.controller.cloneDetector.CloneDetectorController
import io.github.t45k.clione.controller.cloneDetector.create
import io.github.t45k.clione.core.config.RunningConfig
import io.github.t45k.clione.entity.CloneCandidate
import io.github.t45k.clione.entity.CloneSets
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.FileChangeType
import io.github.t45k.clione.entity.IdCloneMap
import io.github.t45k.clione.entity.PathClonesMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.math.max
import kotlin.math.min

class CloneTracker(
    private val git: GitController,
    private val pullRequest: PullRequestController,
    private val config: RunningConfig,
) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val sourceCodePath: Path = git.getProjectPath().resolve(config.src).toRealPath()

    fun track(): TrackingResultGenerator {
        logger.info("[START]\tClone Tracking on ${pullRequest.fullName}/${pullRequest.number}")

        val (oldCommitHash: String, newCommitHash: String) = pullRequest.getComparisonCommits(git)
        val cloneDetector: CloneDetectorController = create(sourceCodePath, config)
        val (oldChangedFiles: Set<Path>, newChangedFiles: Set<Path>) = git.findChangedFiles(
            oldCommitHash,
            newCommitHash
        )

        logger.info("[START]\tNew revision: $newCommitHash")
        git.checkout(newCommitHash)
        val (newCloneSets: CloneSets, newIdCloneMap: IdCloneMap) = cloneDetector.execute(
            newChangedFiles,
            CloneStatus.ADD
        )
        val newPathClonesMap: PathClonesMap = newIdCloneMap.values.groupBy { it.filePath }
        logger.info("[END]\tNew revision: $newCommitHash")

        logger.info("[START]\tOld revision: $oldCommitHash")
        git.checkout(oldCommitHash)
        val (oldCloneSets: CloneSets, oldIdCloneMap: IdCloneMap) = cloneDetector.execute(
            oldChangedFiles,
            CloneStatus.DELETE
        )
        val oldPathClonesMap: PathClonesMap = oldIdCloneMap.values.groupBy { it.filePath }
        logger.info("[END]\tOld revision: $oldCommitHash")

        mapClones(oldPathClonesMap, newPathClonesMap, oldChangedFiles, oldCommitHash, newCommitHash)

        logger.info("[END]\tclone tracking on ${pullRequest.number}")

        val oldTargetClones = filterTargetClones(oldCloneSets, oldIdCloneMap)
        val newTargetClones = filterTargetClones(newCloneSets, newIdCloneMap)
        return TrackingResultGenerator(oldTargetClones, newTargetClones, newIdCloneMap)
    }

    @VisibleForTesting
    fun filterTargetClones(cloneSets: CloneSets, idCloneMap: IdCloneMap): List<List<CloneCandidate>> =
        cloneSets.filterNot { cloneSet ->
            cloneSet.all { (idCloneMap[it] ?: error("")).status == CloneStatus.STABLE }
                || cloneSet.all { (idCloneMap[it] ?: error("")).status == CloneStatus.MODIFY }
                || cloneSet.count { (idCloneMap[it] ?: error("")).status == CloneStatus.DELETE } >= cloneSet.size - 1
        }
            .map { it.map { id -> idCloneMap[id] ?: error("") } }

    @VisibleForTesting
    fun mapClones(
        oldPathClonesMap: PathClonesMap,
        newPathClonesMap: PathClonesMap,
        oldChangedFiles: Set<Path>,
        oldCommitHash: String,
        newCommitHash: String
    ) {
        for ((oldFilePath: Path, clones: List<CloneCandidate>) in oldPathClonesMap.entries) {
            if (!oldChangedFiles.contains(oldFilePath)) {
                continue
            }

            val (type: FileChangeType, addedLines: List<Int>, deletedLines: List<Int>, newFilePath: Path) =
                git.calcFileDiff(oldFilePath, oldCommitHash, newCommitHash)
            if (type == FileChangeType.DELETE) {
                continue
            }

            val candidates: List<CloneCandidate> = newPathClonesMap[newFilePath] ?: emptyList()
            if (addedLines.isEmpty() && deletedLines.isEmpty()) {
                clones.forEach { it.status = CloneStatus.STABLE }
                candidates.forEach { it.status = CloneStatus.STABLE }
            }

            mapClonesInSameFile(clones, candidates, addedLines, deletedLines)
        }
    }

    private fun mapClonesInSameFile(
        clones: List<CloneCandidate>, mappingCandidates: List<CloneCandidate>,
        addedLines: List<Int>, deletedLines: List<Int>
    ) {
        clones.forEach { oldClone ->
            val oldStartLine: Int =
                oldClone.startLine - if (deletedLines.isEmpty()) 0 else deletedLines[oldClone.startLine - 1]
            val oldEndLine: Int =
                oldClone.endLine - if (deletedLines.isEmpty()) 0 else deletedLines[oldClone.endLine - 1]

            mappingCandidates.asSequence()
                .filter { candidate: CloneCandidate ->
                    val newStartLine: Int =
                        candidate.startLine - if (addedLines.isEmpty()) 0 else addedLines[candidate.startLine - 1]
                    val newEndLine: Int =
                        candidate.endLine - if (addedLines.isEmpty()) 0 else addedLines[candidate.endLine - 1]
                    calcLineOverlapping(newStartLine, newEndLine, oldStartLine, oldEndLine) >= 0.3
                }.maxBy { candidate: CloneCandidate ->
                    val newStartLine: Int =
                        candidate.startLine - if (addedLines.isEmpty()) 0 else addedLines[candidate.startLine - 1]
                    val newEndLine: Int =
                        candidate.endLine - if (addedLines.isEmpty()) 0 else addedLines[candidate.endLine - 1]
                    calcLineOverlapping(newStartLine, newEndLine, oldStartLine, oldEndLine)
                }?.let { candidate ->
                    candidate.mapperCloneInstanceId = oldClone.id
                    oldClone.mapperCloneInstanceId = candidate.id

                    if (candidate.tokenSequence == oldClone.tokenSequence) {
                        candidate.status = CloneStatus.STABLE
                        oldClone.status = CloneStatus.STABLE
                    } else {
                        candidate.status = CloneStatus.MODIFY
                        oldClone.status = CloneStatus.MODIFY
                    }
                }
        }
    }

    private fun calcLineOverlapping(start1: Int, end1: Int, start2: Int, end2: Int): Double =
        if (end1 - start1 + end2 - start2 == 0) {
            0.0
        } else {
            2 * (min(end1, end2) - max(start1, start2)).toDouble() / (end1 - start1 + end2 - start2).toDouble()
        }
}
