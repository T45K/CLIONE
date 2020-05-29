package io.github.t45k.clione.controller

import io.reactivex.rxjava3.core.Observable
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import util.deleteRecursive
import java.nio.file.Path

class GitController(private val git: Git) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)

        fun clone(repositoryFullName: String, token: String, number: Int): GitController =
            Observable.just(
                Git.cloneRepository()
                    .setURI("https://github.com/$repositoryFullName.git")
                    .setDirectory(Path.of("storage", "${repositoryFullName}_$number").toFile())
                    .setCredentialsProvider(UsernamePasswordCredentialsProvider("token", token))
                    .setCloneAllBranches(true)
                    .call()
                    .run { GitController(this) }
            )
                .doOnSubscribe { logger.info("[START]\tclone $repositoryFullName") }
                .doOnComplete { logger.info("[END]\tclone $repositoryFullName") }
                .blockingSingle()
    }

    fun deleteRepo() =
        Observable.just(Path.of(git.repository.directory.parentFile.absolutePath))
            .doOnSubscribe { logger.info("[START]\tdelete ${git.repository.directory.parentFile}") }
            .doOnComplete { logger.info("[END]\tdelete ${git.repository.directory.parentFile}") }
            .subscribe(::deleteRecursive)!!
}
