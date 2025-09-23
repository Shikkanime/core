package fr.shikkanime.wrappers

import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsString
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TwitterWrapper {
    data class AuthParams(
        val consumerKey: String,
        val consumerSecret: String,
        val accessToken: String,
        val accessTokenSecret: String,
    )

    enum class MediaCategory(val value: String) {
        TWEET_IMAGE("tweet_image"),
    }

    enum class MediaType(val value: String) {
        IMAGE_JPEG("image/jpeg"),
        ;
    }

    private const val API_URL = "https://api.twitter.com/2"
    private const val ALGORITHM = "HmacSHA1"
    private val secureRandom = SecureRandom()
    private val charset = StandardCharsets.UTF_8
    private val httpRequest = HttpRequest()
    private const val CHUNK_SIZE = 2 * 1024 * 1024

    private fun generateSignature(
        authParams: AuthParams,
        data: String
    ): String {
        val secretKey = "${URLEncoder.encode(authParams.consumerSecret, charset)}&${URLEncoder.encode(authParams.accessTokenSecret, charset)}"

        val mac = Mac.getInstance(ALGORITHM).apply {
            init(SecretKeySpec(secretKey.toByteArray(), ALGORITHM))
        }

        return Base64.getEncoder().encodeToString(mac.doFinal(data.toByteArray()))
    }

    private fun getAuthorizationHeader(
        authParams: AuthParams,
        url: String
    ): String {
        val timestamp = System.currentTimeMillis() / 1000
        val nonce = timestamp + secureRandom.nextInt()

        val headerParams = mapOf(
            "oauth_consumer_key" to authParams.consumerKey,
            "oauth_signature_method" to "HMAC-SHA1",
            "oauth_timestamp" to timestamp.toString(),
            "oauth_nonce" to nonce.toString(),
            "oauth_version" to "1.0",
            "oauth_token" to authParams.accessToken
        ).toMutableMap()

        val signatureBaseString = buildString {
            append("POST")
            append("&")
            append(URLEncoder.encode(url, charset))
            append("&")
            append(URLEncoder.encode(
                headerParams.toSortedMap().entries.joinToString("&") { "${it.key}=${it.value}" },
                charset
            ))
        }

        headerParams["oauth_signature"] = generateSignature(authParams, signatureBaseString)

        return "OAuth " + headerParams.entries.joinToString(", ") {
            "${it.key}=\"${URLEncoder.encode(it.value, charset)}\""
        }
    }

    private suspend fun uploadMediaChunkedInit(
        authParams: AuthParams,
        mediaCategory: MediaCategory,
        mediaType: MediaType,
        totalBytes: Long
    ): String {
        val response = httpRequest.post(
            "$API_URL/media/upload/initialize",
            headers = mapOf(
                HttpHeaders.ContentType to ContentType.Application.Json.toString(),
                HttpHeaders.Authorization to getAuthorizationHeader(authParams, "$API_URL/media/upload/initialize")
            ),
            body = ObjectParser.toJson(
                mapOf(
                    "media_category" to mediaCategory.value,
                    "media_type" to mediaType.value,
                    "total_bytes" to totalBytes
                )
            )
        )

        require(response.status == HttpStatusCode.OK) { "Failed to initialize media upload (${response.status.value} - ${response.bodyAsText()})" }
        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("data").getAsString("id")!!
    }

    private suspend fun uploadMediaChunkedAppend(
        authParams: AuthParams,
        segmentIndex: Long,
        mediaId: String,
        fileName: String,
        segment: ByteArray,
    ) {
        val response = httpRequest.post(
            "$API_URL/media/upload/$mediaId/append",
            headers = mapOf(
                HttpHeaders.ContentType to ContentType.MultiPart.FormData.toString(),
                HttpHeaders.Authorization to getAuthorizationHeader(authParams, "$API_URL/media/upload/$mediaId/append")
            ),
            body = formData {
                append("segment_index", segmentIndex)
                append("media", segment, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=$fileName")
                })
            }
        )

        require(response.status == HttpStatusCode.OK) { "Failed to append media chunk (${response.status.value} - ${response.bodyAsText()})" }
    }

    private suspend fun uploadMediaChunkedFinalize(
        authParams: AuthParams,
        mediaId: String,
    ) {
        val response = httpRequest.post(
            "$API_URL/media/upload/$mediaId/finalize",
            headers = mapOf(
                HttpHeaders.ContentType to ContentType.Application.Json.toString(),
                HttpHeaders.Authorization to getAuthorizationHeader(authParams, "$API_URL/media/upload/$mediaId/finalize")
            )
        )

        require(response.status == HttpStatusCode.OK) { "Failed to finalize media upload (${response.status.value} - ${response.bodyAsText()})" }
    }

    suspend fun uploadMediaChunked(
        authParams: AuthParams,
        mediaCategory: MediaCategory,
        mediaType: MediaType,
        fileName: String,
        bytes: ByteArray
    ): String {
        val mediaId = uploadMediaChunkedInit(authParams, mediaCategory, mediaType, bytes.size.toLong())

        ByteArrayInputStream(bytes).use { inputStream ->
            generateSequence { inputStream.readNBytes(CHUNK_SIZE).takeIf { it.isNotEmpty() } }
                .forEachIndexed { index, segment -> uploadMediaChunkedAppend(authParams, index.toLong(), mediaId, fileName, segment) }
        }

        uploadMediaChunkedFinalize(authParams, mediaId)
        return mediaId
    }

    suspend fun createTweet(
        authParams: AuthParams,
        mediaIds: List<String>? = null,
        inReplyToTweetId: String? = null,
        text: String? = null,
    ): String {
        val parameters = buildMap {
            mediaIds?.let { put("media", mapOf("media_ids" to it)) }
            inReplyToTweetId?.let { put("reply", mapOf("in_reply_to_tweet_id" to it)) }
            text?.let { put("text", it) }
        }

        val response = httpRequest.post(
            "$API_URL/tweets",
            headers = mapOf(
                HttpHeaders.ContentType to ContentType.Application.Json.toString(),
                HttpHeaders.Authorization to getAuthorizationHeader(authParams, "$API_URL/tweets")
            ),
            body = ObjectParser.toJson(parameters)
        )

        require(response.status == HttpStatusCode.Created) { "Failed to create tweet (${response.status.value} - ${response.bodyAsText()})" }
        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("data").getAsString("id")!!
    }
}