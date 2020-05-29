package io.github.t45k.clione.controller

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.nio.file.Path


class GitController(private val git: Git) {
    companion object {
        fun clone(repositoryFullName: String, token: String): GitController =
            Git.cloneRepository()
                .setURI("https://github.com/$repositoryFullName.git")
                .setDirectory(Path.of("storage", repositoryFullName).toFile())
                .setCredentialsProvider(UsernamePasswordCredentialsProvider("token", token))
                .setCloneAllBranches(true)
                .call()
                .run { GitController(this) }
    }
}
