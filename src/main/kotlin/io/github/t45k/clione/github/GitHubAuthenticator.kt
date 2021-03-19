package io.github.t45k.clione.github

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.JsonNode
import io.github.t45k.clione.controller.PullRequestController
import io.github.t45k.clione.entity.NoPropertyFileException
import io.github.t45k.clione.util.DigestUtil
import io.github.t45k.clione.util.minutesAfter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.security.Security
import java.security.interfaces.RSAPrivateKey
import java.util.Date
import java.util.ResourceBundle

class GitHubAuthenticator {
    companion object {
        init {
            Security.addProvider(BouncyCastleProvider())
        }

        private val resourceBundle: ResourceBundle = ResourceBundle.getBundle("github")
            ?: throw NoPropertyFileException("github.properties does not exist")
        private val githubPrivateKey: RSAPrivateKey =
            DigestUtil.getRSAPrivateKeyFromPEMFileContents(resourceBundle.getString("GITHUB_PRIVATE_KEY"))
        private val githubAppIdentifier: String = resourceBundle.getString("GITHUB_APP_IDENTIFIER")

        fun authenticateFromPullRequest(json: JsonNode): Pair<PullRequestController, Token> =
            authenticate(json, json["number"].asInt())

        fun authenticateFromCheckRun(json: JsonNode): Pair<PullRequestController, Token> =
            authenticate(json, json["check_run"]["pull_requests"][0]["number"].asInt())

        /**
         * Authenticate GitHub App with json (from RequestBody).
         *
         * @param json JSON
         *
         * @return PullRequest information and token for 'git clone'
         */
        private fun authenticate(json: JsonNode, pullRequestNumber: Int): Pair<PullRequestController, Token> {
            val token: String = authenticateApp()
                .run { generateToken(this, json["installation"]["id"].asLong()) }

            return GitHubBuilder()
                .withAppInstallationToken(token)
                .build()
                .getRepository(json["repository"]["full_name"].asText())
                .getPullRequest(pullRequestNumber)
                .run { PullRequestController(this) } to token
        }

        /**
         * Instantiate a GitHub client authenticated as a GitHub App.
         * GitHub App authentication requires that you construct a
         * JWT (https://jwt.io/introduction/) signed with the app's private key,
         * so GitHub can be sure that it came from the app and was not altered by
         * a malicious third party.
         */
        private fun authenticateApp(): GitHub {
            val nowTime = Date()
            val expiredTime: Date = nowTime.minutesAfter(10)

            val jwt: String = JWT.create()
                .withIssuedAt(nowTime)
                .withExpiresAt(expiredTime)
                .withIssuer(githubAppIdentifier)
                .sign(Algorithm.RSA256(null, githubPrivateKey))
            return GitHubBuilder().withJwtToken(jwt).build()
        }

        /**
         * Generate installation token to instantiate a GitHub client, authenticated as an installation of a
         * GitHub App, to run API operations.
         */
        @Suppress("DEPRECATION")
        private fun generateToken(appClient: GitHub, installationId: Long): Token =
            appClient.app.getInstallationById(installationId)
                .createToken()
                .create()
                .token
    }

}

typealias Token = String
