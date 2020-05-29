package io.github.t45k.clione.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.t45k.clione.exception.NoPropertyFileExistsException
import io.github.t45k.clione.github.GitHubAuthenticator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import util.DigestUtil
import java.util.ResourceBundle
import javax.servlet.http.HttpServletRequest

@RestController
class ClioneApiController {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val githubWebhookSecret: String

    init {
        val bundle: ResourceBundle = ResourceBundle.getBundle("verify") ?: throw NoPropertyFileExistsException()
        githubWebhookSecret = bundle.getString("GITHUB_WEBHOOK_SECRET")
    }

    companion object {
        const val WEBHOOK_SIGNATURE: String = "x-hub-signature"
        const val WEBHOOK_EVENT: String = "x-github-event"
    }

    @Autowired
    private lateinit var request: HttpServletRequest

    @GetMapping("")
    fun home(): String = "hello"

    @PostMapping("/event_handler")
    fun postEventHandler(@RequestBody rawRequestBody: String) {
        logger.info("Event was received")
        if (!verifyWebhookSignature(rawRequestBody)) {
            return
        }

        val json: JsonNode = ObjectMapper().readTree(rawRequestBody)
        if (!isPullRequestOpen(request.getHeader(WEBHOOK_EVENT), json["action"].asText())) {
            return
        }

        val repositoryFullName = json["repository"]["full_name"].asText()
        logger.info("---- received pull request open from $repositoryFullName")

        val pullRequestNumber: Int = json["number"].asInt()
        val (pullRequest: PullRequestController, token: String) = GitHubAuthenticator.authenticate(json)
        val git: GitController = GitController.clone(repositoryFullName, token, pullRequestNumber)

        pullRequest.comment("hello")
        git.deleteRepo()
    }

    private fun isPullRequestOpen(event: String, action: String): Boolean =
        event == "pull_request" && action == "opened"

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
