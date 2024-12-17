package fr.shikkanime.wrappers

import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ThreadsWrapper {
    enum class PostType {
        TEXT,
        IMAGE,
        VIDEO,
    }

    private const val AUTHORIZATION_URL = "https://www.threads.net"
    private const val API_URL = "https://graph.threads.net"
    private val httpRequest = HttpRequest()

    private fun getRedirectUri() = "${Constant.baseUrl}/api/threads".replace("http://", "https://")

    fun getCode(appId: String) = "$AUTHORIZATION_URL/oauth/authorize?" +
            "client_id=$appId&" +
            "redirect_uri=${getRedirectUri()}&" +
            "response_type=code&" +
            "scope=threads_basic,threads_content_publish"

    suspend fun getAccessToken(appId: String, appSecret: String, code: String): String {
        val response = httpRequest.post(
            "$API_URL/oauth/access_token?" +
                    "client_id=$appId&" +
                    "client_secret=$appSecret&" +
                    "redirect_uri=${getRedirectUri()}&" +
                    "code=$code&" +
                    "grant_type=authorization_code",
            headers = mapOf(HttpHeaders.ContentType to ContentType.Application.Json.toString()),
        )

        require(response.status == HttpStatusCode.OK) { "Failed to get token" }

        val json = ObjectParser.fromJson(response.bodyAsText())
        return json["access_token"].asString
    }

    suspend fun getLongLivedAccessToken(appSecret: String, accessToken: String): String {
        val response = httpRequest.get(
            "$API_URL/access_token?" +
                    "client_secret=$appSecret&" +
                    "access_token=$accessToken&" +
                    "grant_type=th_exchange_token",
        )

        require(response.status == HttpStatusCode.OK) { "Failed to get long-lived token" }

        val json = ObjectParser.fromJson(response.bodyAsText())
        return json["access_token"].asString
    }

    suspend fun post(
        accessToken: String,
        postType: PostType,
        text: String,
        imageUrl: String? = null,
        altText: String? = null,
        replyToId: Long? = null
    ): Long {
        val parameters = mapOf(
            "access_token" to accessToken,
            "media_type" to postType.name,
            "text" to URLEncoder.encode(text, StandardCharsets.UTF_8),
            "image_url" to imageUrl,
            "alt_text" to altText,
            "reply_to_id" to replyToId,
        ).filterValues { it != null }.map { (key, value) -> "$key=$value" }.joinToString("&")

        val createResponse = httpRequest.post(
            "$API_URL/me/threads?$parameters",
            headers = mapOf(HttpHeaders.ContentType to ContentType.Application.Json.toString()),
        )

        require(createResponse.status == HttpStatusCode.OK) { "Failed to post" }
        val creationId = ObjectParser.fromJson(createResponse.bodyAsText())["id"].asString

        val publishResponse = httpRequest.post(
            "$API_URL/me/threads_publish?" +
                    "access_token=$accessToken&" +
                    "creation_id=$creationId",
            headers = mapOf(HttpHeaders.ContentType to ContentType.Application.Json.toString()),
        )

        require(publishResponse.status == HttpStatusCode.OK) { "Failed to publish" }
        return ObjectParser.fromJson(publishResponse.bodyAsText())["id"].asLong
    }
}