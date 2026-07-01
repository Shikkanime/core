package fr.shikkanime.wrappers

import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object ThreadsWrapper {
    enum class Status {
        EXPIRED,
        ERROR,
        FINISHED,
        IN_PROGRESS,
        PUBLISHED
    }

    enum class PostType {
        TEXT,
        IMAGE
    }

    private const val AUTHORIZATION_URL = "https://www.threads.net"
    private const val API_URL = "https://graph.threads.net"
    private const val API_VERSION = "v1.0"

    private fun getRedirectUri() = "${Constant.baseUrl}$ADMIN/api/threads".replace("http://", "https://")

    fun getCode(appId: String) = "$AUTHORIZATION_URL/oauth/authorize?" +
            "client_id=$appId&" +
            "redirect_uri=${getRedirectUri()}&" +
            "response_type=code&" +
            "scope=threads_basic,threads_content_publish"

    suspend fun getAccessToken(appId: String, appSecret: String, code: String): String {
        val response = HttpRequest.post(
            "$API_URL/oauth/access_token?" +
                    "client_id=$appId&" +
                    "client_secret=$appSecret&" +
                    "redirect_uri=${getRedirectUri()}&" +
                    "code=$code&" +
                    "grant_type=authorization_code",
            headers = mapOf(HttpHeaders.ContentType to ContentType.Application.Json.toString()),
        )

        require(response.status == HttpStatusCode.OK) { "Failed to get token (${response.status.value} - ${response.bodyAsText()})" }
        return ObjectParser.fromJson(response.bodyAsText())["access_token"].asString
    }

    suspend fun getLongLivedAccessToken(appSecret: String, accessToken: String): String {
        val response = HttpRequest.get(
            "$API_URL/access_token?" +
                    "client_secret=$appSecret&" +
                    "access_token=$accessToken&" +
                    "grant_type=th_exchange_token",
        )

        require(response.status == HttpStatusCode.OK) { "Failed to get long-lived token (${response.status.value} - ${response.bodyAsText()})" }
        return ObjectParser.fromJson(response.bodyAsText())["access_token"].asString
    }

    suspend fun createThreads(
        accessToken: String,
        postType: PostType,
        text: String,
        imageUrl: String? = null,
        altText: String? = null,
        replyToId: Long? = null
    ): Long {
        val parameters = buildMap<String, Any> {
            put("access_token", accessToken)
            put("media_type", postType.name)
            put("text", URLEncoder.encode(text, StandardCharsets.UTF_8))

            imageUrl?.let { put("image_url", URLEncoder.encode(it, StandardCharsets.UTF_8)) }
            altText?.let { put("alt_text", URLEncoder.encode(it, StandardCharsets.UTF_8)) }
            replyToId?.let { put("reply_to_id", it) }
        }.map { (key, value) -> "$key=$value" }
            .joinToString("&")

        val response = HttpRequest.post("$API_URL/$API_VERSION/me/threads?$parameters", headers = mapOf(HttpHeaders.ContentType to ContentType.Application.Json.toString()))
        require(response.status == HttpStatusCode.OK) { "Failed to post (${response.status.value} - ${response.bodyAsText()})" }
        return ObjectParser.fromJson(response.bodyAsText())["id"].asLong
    }

    suspend fun getThreadsStatus(
        accessToken: String,
        containerId: Long
    ): Status {
        val response = HttpRequest.get("$API_URL/$API_VERSION/$containerId?access_token=$accessToken&fields=status,error_message")
        require(response.status == HttpStatusCode.OK) { "Failed to publish (${response.status.value} - ${response.bodyAsText()})" }
        return Status.valueOf(ObjectParser.fromJson(response.bodyAsText())["status"].asString)
    }

    suspend fun threadsPublish(
        accessToken: String,
        containerId: Long
    ): Long {
        val response = HttpRequest.post("$API_URL/$API_VERSION/me/threads_publish?access_token=$accessToken&creation_id=$containerId")
        require(response.status == HttpStatusCode.OK) { "Failed to publish (${response.status.value} - ${response.bodyAsText()})" }
        return ObjectParser.fromJson(response.bodyAsText())["id"].asLong
    }

    private suspend fun waitUntilContainerReadyForPublishing(accessToken: String, containerId: Long) {
        delay(30.seconds)

        var status = getThreadsStatus(accessToken, containerId)
        check(status != Status.ERROR) { "Failed to publish container $containerId due to error status" }

        for (attempt in 1..5) {
            if (status != Status.IN_PROGRESS) {
                return
            }

            delay(attempt.minutes)
            status = getThreadsStatus(accessToken, containerId)
            check(status != Status.ERROR) { "Failed to publish container $containerId due to error status" }
        }
    }

    suspend fun post(
        accessToken: String,
        postType: PostType,
        text: String,
        imageUrl: String? = null,
        altText: String? = null,
        replyToId: Long? = null
    ): Long {
        val containerId = createThreads(accessToken, postType, text, imageUrl, altText, replyToId)
        waitUntilContainerReadyForPublishing(accessToken, containerId)
        return threadsPublish(accessToken, containerId)
    }
}