package io.github.t45k.clione.core

import com.google.common.annotations.VisibleForTesting
import io.github.t45k.clione.controller.GitController
import io.github.t45k.clione.controller.PullRequestController
import io.github.t45k.clione.controller.cloneDetector.CloneDetectorController
import io.github.t45k.clione.controller.cloneDetector.create
import io.github.t45k.clione.entity.CloneInstance
import io.github.t45k.clione.entity.CloneSets
import io.github.t45k.clione.entity.CloneStatus
import io.github.t45k.clione.entity.FileChangeType
import io.github.t45k.clione.entity.FileClonesMap
import io.github.t45k.clione.entity.IdCloneMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.math.max
import kotlin.math.min

class CloneTracker(private val git: GitController, private val pullRequest: PullRequestController,
                   private val config: RunningConfig) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val sourceCodePath: Path = git.getProjectPath().resolve(config.infix).toRealPath()

    fun track(): Pair<List<List<CloneInstance>>, List<List<CloneInstance>>> {
        logger.info("[START]\tClone Tracking on ${pullRequest.fullName}/${pullRequest.number}")

        val (oldCommitHash: String, newCommitHash: String) = pullRequest.getComparisonCommits()
        val cloneDetector: CloneDetectorController = create(sourceCodePath, config)
        val (oldChangedFiles: Set<String>, newChangedFiles: Set<String>) = git.findChangedFiles(oldCommitHash, newCommitHash)

        logger.info("[START]\tNew revision: $newCommitHash")
        git.checkout(newCommitHash)
        val (newCloneSets: CloneSets, newIdCloneMap: IdCloneMap) = cloneDetector.execute(newChangedFiles, CloneStatus.ADD)
        val newFileClonesMap: FileClonesMap = newIdCloneMap.values.groupBy { it.fileName }
        logger.info("[END]\tNew revision: $newCommitHash")

        logger.info("[START]\tOld revision: $oldCommitHash")
        git.checkout(oldCommitHash)
        val (oldCloneSets: CloneSets, oldIdCloneMap: IdCloneMap) = cloneDetector.execute(oldChangedFiles, CloneStatus.DELETE)
        val oldFileClonesMap: FileClonesMap = oldIdCloneMap.values.groupBy { it.fileName }
        logger.info("[END]\tOld revision: $oldCommitHash")

        mapClones(oldFileClonesMap, newFileClonesMap, oldChangedFiles, oldCommitHash, newCommitHash)

        logger.info("[END]\tclone tracking on ${pullRequest.number}")

        return filterInconsistentChange(oldCloneSets, oldIdCloneMap) to filterInconsistentChange(newCloneSets, newIdCloneMap)
    }

    @VisibleForTesting
    fun filterInconsistentChange(cloneSets: CloneSets, idCloneMap: IdCloneMap): List<List<CloneInstance>> =
        cloneSets.filterNot { cloneSet ->
            cloneSet.all { (idCloneMap[it] ?: error("")).status == CloneStatus.STABLE }
                || cloneSet.all { (idCloneMap[it] ?: error("")).status == CloneStatus.MODIFY }
        }
            .map { it.map { id -> idCloneMap[id] ?: error("") } }

    @VisibleForTesting
    fun mapClones(oldFileClonesMap: FileClonesMap, newFileClonesMap: FileClonesMap, oldChangedFiles: Set<String>,
                  oldCommitHash: String, newCommitHash: String) {
        for ((oldFilesPath: String, clones: List<CloneInstance>) in oldFileClonesMap.entries) {
            if (!oldChangedFiles.contains(oldFilesPath)) {
                continue
            }

            val (type: FileChangeType, lineMapping: List<Int>, newFileName: String) =
                git.calcFileDiff(oldFilesPath, oldCommitHash, newCommitHash)
            val candidates: List<CloneInstance> = newFileClonesMap[newFileName] ?: emptyList()
            if (type == FileChangeType.DELETE) {
                continue
            } else if (lineMapping.isEmpty()) {
                clones.forEach { it.status = CloneStatus.STABLE }
                candidates.forEach { it.status = CloneStatus.STABLE }
            }

            mapClonesInSameFile(clones, candidates, lineMapping)
        }
    }

    private fun mapClonesInSameFile(clones: List<CloneInstance>, mappingCandidates: List<CloneInstance>, lineMapping: List<Int>) {
        clones.forEach { oldClone ->
            val mappedStartLine: Int = (if (lineMapping.isEmpty()) 0 else lineMapping[oldClone.startLine]) + oldClone.startLine
            val mappedEndLine: Int = (if (lineMapping.isEmpty()) 0 else lineMapping[oldClone.endLine]) + oldClone.endLine

            mappingCandidates.asSequence()
                .filter { candidate ->
                    calcLineOverlapping(candidate.startLine, candidate.endLine, mappedStartLine, mappedEndLine)
                        .let { max(it.first, it.second) >= 0.7 }
                }.maxBy { candidate ->
                    calcLineOverlapping(candidate.startLine, candidate.endLine, mappedStartLine, mappedEndLine)
                        .let { 2 * it.first * it.second / (it.first + it.second) }
                }
                ?.let {
                    it.mapperCloneInstanceId = oldClone.id
                    oldClone.mapperCloneInstanceId = it.id

                    if (it.tokenSequence == oldClone.tokenSequence) {
                        it.status = CloneStatus.STABLE
                        oldClone.status = CloneStatus.STABLE
                    } else {
                        it.status = CloneStatus.MODIFY
                        oldClone.status = CloneStatus.MODIFY
                    }
                }
        }
    }

    private fun calcLineOverlapping(start1: Int, end1: Int, start2: Int, end2: Int): Pair<Double, Double> =
        (min(end1, end2) - max(start1, start2) + 1).toDouble() / (end1 - start1 + 1).toDouble() to
            (min(end1, end2) - max(start1, start2) + 1).toDouble() / (end2 - start2 + 1).toDouble()
}
