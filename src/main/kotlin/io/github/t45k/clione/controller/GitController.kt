package io.github.t45k.clione.controller

import io.github.t45k.clione.entity.FileChangeType
import io.github.t45k.clione.entity.FileDiff
import io.github.t45k.clione.util.EMPTY_NAME_PATH
import io.github.t45k.clione.util.deleteRecursively
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

        fun cloneIfNotExists(
            repositoryFullName: String,
            token: String,
            pullRequest: PullRequestController
        ): GitController =
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
                        GitController(this)
                    }
                }
                .doOnSubscribe { logger.info("[START]\tclone $repositoryFullName") }
                .doOnComplete { logger.info("[END]\tclone $repositoryFullName") }
                .blockingSingle()

        fun cloneIfNotExists(
            repositoryFullName: String,
            userName: String,
            oAuthToken: String,
            defaultBranchName: String
        ): GitController =
            Observable.just(Path.of("storage/$repositoryFullName/.git"))
                .map {
                    if (Files.exists(it)) {
                        FileRepository(it.toString())
                            .run { Git(this) }
                            .apply { this.checkout().setName(defaultBranchName).call() }
                            .apply { this.pull().call() }
                    } else {
                        Git.cloneRepository()
                            .setURI("https://github.com/$repositoryFullName.git")
                            .setDirectory(it.parent.toFile())
                            .setCredentialsProvider(UsernamePasswordCredentialsProvider(userName, oAuthToken))
                            .setCloneAllBranches(true)
                            .call()
                    }.run {
                        GitController(this)
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
            .subscribe{it.deleteRecursively()}
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
                    diffEntry.oldPath?.apply { if (this != "/dev/null") oldChangedFiles.add(completePath(this)) }
                    diffEntry.newPath?.apply { if (this != "/dev/null") newChangedFiles.add(completePath(this)) }
                    oldChangedFiles to newChangedFiles
                })
            .let { it.first to it.second }


    /**
     * This method must be called about a changed file.
     * Current revision should be old.
     *
     * If a change between the commits is only change of file name, line mapping is empty
     */
    fun calcFileDiff(filePath: Path, oldCommitHash: String, newCommitHash: String): FileDiff {
        checkout(oldCommitHash)
        val fileName: String = repositoryPath.relativize(filePath).toString()
        val entry: DiffEntry = executeDiffCommand(oldCommitHash, newCommitHash).first { it.oldPath == fileName }

        if (entry.changeType == DiffEntry.ChangeType.DELETE) {
            return FileDiff(FileChangeType.DELETE, emptyList(), emptyList(), EMPTY_NAME_PATH)
        } else if (entry.changeType == DiffEntry.ChangeType.ADD) {
            return FileDiff(FileChangeType.ADD, emptyList(), emptyList(), EMPTY_NAME_PATH)
        }

        val oldRawText: RawText = readBlob(entry.oldId)
        val newRawText: RawText = readBlob(entry.newId)
        val editList: EditList = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS)
            .diff(RawTextComparator.DEFAULT, oldRawText, newRawText)

        if (editList.isEmpty()) {
            return FileDiff(FileChangeType.MODIFY, emptyList(), emptyList(), completePath(entry.newPath))
        }

        val oldSize: Int = String(oldRawText.rawContent).split("\n").size
        val newSize: Int = String(newRawText.rawContent).split("\n").size
        val (addedLines, deletedLines) = mapLine(editList, oldSize, newSize)
        return FileDiff(FileChangeType.MODIFY, addedLines, deletedLines, completePath(entry.newPath))
    }

    private fun completePath(relativeFileLocation: String): Path = repositoryPath.resolve(relativeFileLocation)

    /**
     * Calculate added and deleted lines.
     * When clone tracking is executed, lines of new clone instances are subtracted add lines
     * and lines of old clone instances are subtracted deleted lines
     */
    private fun mapLine(editList: EditList, oldSize: Int, newSize: Int): Pair<List<Int>, List<Int>> {
        val addedLines: Array<Int> = Array(newSize + 1) { 0 }
        val deletedLines: Array<Int> = Array(oldSize + 1) { 0 }

        for (edit: Edit in editList) {
            when (edit.type) {
                Edit.Type.INSERT -> (edit.beginB + 1..edit.endB).forEach { addedLines[it]++ }

                Edit.Type.DELETE -> (edit.beginA + 1..edit.endA).forEach { deletedLines[it]++ }

                Edit.Type.REPLACE -> {
                    (edit.beginB + 1..edit.endB).forEach { addedLines[it]++ }
                    (edit.beginA + 1..edit.endA).forEach { deletedLines[it]++ }
                }

                Edit.Type.EMPTY -> {
                    // do nothing
                }

                else -> throw RuntimeException()
            }
        }

        (1 until addedLines.size).forEach { addedLines[it] += addedLines[it - 1] }
        (1 until deletedLines.size).forEach { deletedLines[it] += deletedLines[it - 1] }
        return addedLines.toList() to deletedLines.toList()
    }

    private fun readBlob(blobId: AbbreviatedObjectId): RawText =
        git.repository.newObjectReader()
            .open(blobId.toObjectId(), Constants.OBJ_BLOB)
            .run { RawText(this.cachedBytes) }

    /**
     * Same operation as "git diff revision1...revision2"
     */
    private fun executeDiffCommand(oldCommitHash: String, newCommitHash: String): List<DiffEntry> {
        val oldTreeParser: AbstractTreeIterator =
            prepareTreeParser(ObjectId.fromString(getCommonAncestorCommit(oldCommitHash, newCommitHash)))
        val newTreeParser: AbstractTreeIterator = prepareTreeParser(ObjectId.fromString(newCommitHash))

        return DiffFormatter(DisabledOutputStream.INSTANCE)
            .apply { this.setRepository(git.repository) }
            .apply { this.setDiffComparator(RawTextComparator.DEFAULT) }
            .apply { this.isDetectRenames = true }
            .scan(oldTreeParser, newTreeParser)
    }

    fun getParentCommit(commitHash: String): String =
        git.repository
            .parseCommit(ObjectId.fromString(commitHash))
            .parents[0]
            .name

    fun getCommonAncestorCommit(oldCommitHash: String, newCommitHash: String): String =
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
