package io.github.t45k.clione.controller

import io.github.t45k.clione.entity.FileChangeType
import io.github.t45k.clione.entity.FileDiff
import io.github.t45k.clione.util.EMPTY_NAME_PATH
import io.github.t45k.clione.util.deleteRecursive
import io.reactivex.rxjava3.core.Observable
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffAlgorithm
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.Edit
import org.eclipse.jgit.diff.EditList
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.util.io.DisabledOutputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class GitController(private val git: Git) : AutoCloseable {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)

        fun cloneIfNotExists(repositoryFullName: String, token: String, pullRequest: PullRequestController): GitController =
            Observable.just(Path.of("storage/${repositoryFullName}_${pullRequest.number}/.git"))
                .map {
                    if (Files.exists(it)) {
                        FileRepository(it.toString())
                            .run { Git(this) }
                            .apply { this.pull().call() }
                    } else {
                        Git.cloneRepository()
                            .setURI("https://github.com/$repositoryFullName.git")
                            .setDirectory(it.parent.toFile())
                            .setCredentialsProvider(UsernamePasswordCredentialsProvider("token", token))
                            .setCloneAllBranches(true)
                            .call()
                    }.run {
                        GitController(this).apply { this.checkout(pullRequest.headCommitHash) }
                    }
                }
                .doOnSubscribe { logger.info("[START]\tclone $repositoryFullName") }
                .doOnComplete { logger.info("[END]\tclone $repositoryFullName") }
                .blockingSingle()
    }

    private val repositoryPath: Path = git.repository.directory.parentFile.absoluteFile.toPath().toRealPath()

    override fun close() {
        Observable.just(repositoryPath)
            .doOnSubscribe { logger.info("[START]\tdelete ${git.repository.directory.parentFile}") }
            .doOnComplete { logger.info("[END]\tdelete ${git.repository.directory.parentFile}") }
            .subscribe(::deleteRecursive)!!
    }

    fun getProjectPath(): Path = repositoryPath

    /**
     * Same operation as "git checkout hash"
     */
    fun checkout(commitHash: String) {
        git.checkout()
            .setName(commitHash)
            .call()
    }

    /**
     * Execute `git diff oldCommitHash..newCommitHash` and collect changed files in each revision
     */
    fun findChangedFiles(oldCommitHash: String, newCommitHash: String): Pair<Set<Path>, Set<Path>> =
        executeDiffCommand(oldCommitHash, newCommitHash)
            .fold(mutableSetOf<Path>() to mutableSetOf<Path>(),
                { (oldChangedFiles, newChangedFiles), diffEntry ->
                    diffEntry.oldPath?.apply { oldChangedFiles.add(completePath(this)) }
                    diffEntry.newPath?.apply { newChangedFiles.add(completePath(this)) }
                    oldChangedFiles to newChangedFiles
                })
            .let { it.first to it.second }


    /**
     * This method must be called about a changed file.n
     *
     * If a change between the commits is only change of file name, line mapping is empty
     */
    fun calcFileDiff(filePath: Path, oldCommitHash: String, newCommitHash: String): FileDiff {
        checkout(oldCommitHash)
        val fileName: String = repositoryPath.relativize(filePath).toString()
        val entry: DiffEntry = executeDiffCommand(oldCommitHash, newCommitHash).first { it.oldPath == fileName }

        if (entry.changeType == DiffEntry.ChangeType.DELETE) {
            return FileDiff(FileChangeType.DELETE, emptyList(), EMPTY_NAME_PATH)
        } else if (entry.changeType == DiffEntry.ChangeType.ADD) {
            return FileDiff(FileChangeType.ADD, emptyList(), EMPTY_NAME_PATH)
        }

        val oldRawText: RawText = readBlob(entry.oldId)
        val newRawText: RawText = readBlob(entry.newId)
        val editList: EditList = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS)
            .diff(RawTextComparator.DEFAULT, oldRawText, newRawText)

        if (editList.isEmpty()) {
            return FileDiff(FileChangeType.MODIFY, emptyList(), completePath(entry.newPath))
        }
        val size: Int = String(oldRawText.rawContent).split("\n").size
        return FileDiff(FileChangeType.MODIFY, mapLine(editList, size), completePath(entry.newPath))
    }

    private fun completePath(relativeFileLocation: String): Path = repositoryPath.resolve(relativeFileLocation)

    /**
     * Mapping line number of the file of old revision to new one
     * Each value of list is increase/decrease value between old line number and new one
     */
    private fun mapLine(editList: EditList, size: Int): List<Int> {
        val lineMapping: Array<Int> = Array(size + 1) { 0 }

        for (edit: Edit in editList) {
            when (edit.type) {
                Edit.Type.INSERT -> lineMapping[edit.beginA + 1] += edit.endB - edit.beginB

                Edit.Type.DELETE -> (edit.beginA + 1..edit.endA).forEach { lineMapping[it]-- }

                Edit.Type.REPLACE -> {
                    lineMapping[edit.beginA + 1] += edit.endB - edit.beginB
                    (edit.beginA + 1..edit.endA).forEach { lineMapping[it]-- }
                }

                Edit.Type.EMPTY -> {
                    // do nothing
                }

                else -> throw RuntimeException()
            }
        }

        (1..size).forEach { lineMapping[it] += lineMapping[it - 1] }
        return lineMapping.toList()
    }

    private fun readBlob(blobId: AbbreviatedObjectId): RawText =
        git.repository.newObjectReader()
            .open(blobId.toObjectId(), Constants.OBJ_BLOB)
            .run { RawText(this.cachedBytes) }

    /**
     * Same operation as "git diff revision1...revision2"
     */
    private fun executeDiffCommand(oldCommitHash: String, newCommitHash: String): List<DiffEntry> {
        val oldTreeParser: AbstractTreeIterator = prepareTreeParser(ObjectId.fromString(getCommonAncestorCommit(oldCommitHash, newCommitHash)))
        val newTreeParser: AbstractTreeIterator = prepareTreeParser(ObjectId.fromString(newCommitHash))

        return DiffFormatter(DisabledOutputStream.INSTANCE)
            .apply { this.setRepository(git.repository) }
            .apply { this.setDiffComparator(RawTextComparator.DEFAULT) }
            .apply { this.isDetectRenames = true }
            .scan(oldTreeParser, newTreeParser)
    }

    private fun getCommonAncestorCommit(oldCommitHash: String, newCommitHash: String): String =
        RevWalk(git.repository)
            .apply { this.revFilter = RevFilter.MERGE_BASE }
            .apply { this.markStart(this.parseCommit(ObjectId.fromString(oldCommitHash))) }
            .apply { this.markStart(this.parseCommit(ObjectId.fromString(newCommitHash))) }
            .next()
            .name

    private fun prepareTreeParser(objectId: ObjectId): AbstractTreeIterator {
        val walk: RevWalk = RevWalk(git.repository).apply { this.revFilter = RevFilter.MERGE_BASE }
        val commit: RevCommit = walk.parseCommit(objectId)
        val tree: RevTree = walk.parseTree(commit.tree.id)
        val treeParser: CanonicalTreeParser = CanonicalTreeParser()
            .apply { this.reset(git.repository.newObjectReader(), tree) }
        walk.dispose()
        return treeParser
    }
}
