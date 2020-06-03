package io.github.t45k.clione.github

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.JsonNode
import io.github.t45k.clione.controller.PullRequestController
import io.github.t45k.clione.entity.NoPropertyFileExistsException
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import util.DigestUtil
import util.minutesAfter
import java.security.Security
import java.security.interfaces.RSAPrivateKey
import java.util.Date
import java.util.ResourceBundle

class GitHubAuthenticator {
    companion object {
        init {
            Security.addProvider(BouncyCastleProvider())
        }

        private val bundle: ResourceBundle = ResourceBundle.getBundle("github")
            ?: throw NoPropertyFileExistsException()
        private val githubPrivateKey: RSAPrivateKey = DigestUtil.getRSAPrivateKeyFromPEMFileContents(bundle.getString("GITHUB_PRIVATE_KEY"))
        private val githubAppIdentifier: String = bundle.getString("GITHUB_APP_IDENTIFIER")

        fun authenticate(json: JsonNode): Pair<PullRequestController, String> {
            val token: String = authenticateApp()
                .run { generateToken(this, json["installation"]["id"].asLong()) }
            return GitHubBuilder()
                .withAppInstallationToken(token)
                .build()
                .getRepository(json["repository"]["full_name"].asText())
                .getPullRequest(json["number"].asInt())
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
        private fun generateToken(appClient: GitHub, installationId: Long): String =
            appClient.app.getInstallationById(installationId)
                .createToken()
                .create()
                .token
    }
}