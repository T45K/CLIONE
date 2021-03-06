package io.github.t45k.clione.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.t45k.clione.core.CloneTracker
import io.github.t45k.clione.core.TrackingResult
import io.github.t45k.clione.core.config.ConfigGeneratorFactory
import io.github.t45k.clione.core.config.RunningConfig
import io.github.t45k.clione.core.config.Style
import io.github.t45k.clione.entity.NoPropertyFileException
import io.github.t45k.clione.github.GitHubAuthenticator
import io.github.t45k.clione.util.DigestUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView
import java.nio.file.Files
import java.util.ResourceBundle
import javax.servlet.http.HttpServletRequest

@RestController
class ClioneApiController {

    companion object {
        const val WEBHOOK_SIGNATURE: String = "x-hub-signature"
        const val WEBHOOK_EVENT: String = "x-github-event"

        private val logger: Logger = LoggerFactory.getLogger(this::class.java)

        private val githubWebhookSecret: String = ResourceBundle.getBundle("verify")
            ?.getString("GITHUB_WEBHOOK_SECRET")
            ?: throw NoPropertyFileException("verify.properties does not exist")

        private const val CONFIGURATION_LOCATION = ".clione/config.toml"
    }

    @Autowired
    private lateinit var request: HttpServletRequest

    @GetMapping("")
    fun home(): RedirectView = RedirectView("https://github.com/T45K/CLIONE")

    @PostMapping("/event_handler")
    fun postEventHandler(@RequestBody rawRequestBody: String) {
        if (!verifyWebhookSignature(rawRequestBody)) {
            return
        }

        val json: JsonNode = ObjectMapper().readTree(rawRequestBody)
        val isPullRequestOpened: Boolean = request.getHeader(WEBHOOK_EVENT) == "pull_request"
            && json["action"].asText() == "opened"
        val isCheckRunOpened: Boolean = request.getHeader(WEBHOOK_EVENT) == "check_run"
            && json["action"].asText() == "requested_action"
            && json["requested_action"]["identifier"].asText() == "rerun"

        val (pullRequest: PullRequestController, token: String) =
            when {
                isPullRequestOpened -> GitHubAuthenticator.authenticateFromPullRequest(json)
                isCheckRunOpened -> GitHubAuthenticator.authenticateFromCheckRun(json)
                else -> return
            }

        val repositoryFullName = json["repository"]["full_name"].asText()
        logger.info("Received pull request open from $repositoryFullName")

        try {
            GitController.cloneIfNotExists(repositoryFullName, token, pullRequest).use { git ->
                git.checkout(pullRequest.headCommitHash)
                val config: RunningConfig =
                    if (Files.exists(git.getProjectPath().resolve(CONFIGURATION_LOCATION))) {
                        git.getProjectPath().resolve(CONFIGURATION_LOCATION)
                            .let(Files::readString)
                            .let(ConfigGeneratorFactory::fromToml)
                    } else {
                        logger.info("$repositoryFullName doesn't have config.toml")
                        return
                    }

                if (config.style == Style.NONE) {
                    logger.info("$repositoryFullName specifies none style")
                    return
                }

                pullRequest.sendInProgressStatus()
                val cloneTracker = CloneTracker(git, pullRequest, config)
                val trackingResult: TrackingResult = cloneTracker.track().generateResult()
                pullRequest.comment(trackingResult)
                pullRequest.sendSuccessStatus(trackingResult.summarize())
            }
        } catch (e: Exception) {
            val errorMessage: String = e.toString() + "\n\t" + e.stackTrace.joinToString("\n\t")
            logger.error(errorMessage)
            pullRequest.errorComment()
            pullRequest.sendErrorStatus(errorMessage)
        }
    }

    /**
     * Check X-Hub-Signature to confirm that this webhook was generated by
     * GitHub, and not a malicious third party.
     *
     * GitHub uses the WEBHOOK_SECRET, registered to the GitHub App, to
     * create the hash signature sent in the `X-HUB-Signature` header of each
     * webhook. This code computes the expected hash signature and compares it to
     * the signature sent in the `X-HUB-Signature` header. If they don't match,
     * this request is an attack, and you should reject it. GitHub uses the HMAC
     * hexdigest to compute the signature. The `X-HUB-Signature` looks something
     * like this: "sha1=123456".
     * See https://developer.github.com/webhooks/securing/ for details.
     *
     * @param rawBody raw request body
     *
     * @return verification is OK or NO
     */
    private fun verifyWebhookSignature(rawBody: String): Boolean {
        val requestHeader: String = request.getHeader(WEBHOOK_SIGNATURE)
        val (algorithm: String, requestDigest: String) = requestHeader.split("=")
        val myDigest: String = DigestUtil.digest("hmac$algorithm", githubWebhookSecret, rawBody)
        return if (requestDigest == myDigest) {
            true
        } else {
            logger.error("Unauthorized Access")
            false
        }
    }
}
