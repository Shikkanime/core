package fr.shikkanime.wrappers

import com.google.gson.JsonObject
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object BskyWrapper {
    data class Image(
        val image: JsonObject,
        val alt: String = "",
    )

    private const val BASE_URL = "https://bsky.social/xrpc"
    private val contentType = ContentType.Application.Json

    suspend fun createSession(identifier: String, password: String): JsonObject {
        val response = HttpRequest().post(
            "$BASE_URL/com.atproto.server.createSession",
            headers = mapOf(
                HttpHeaders.ContentType to contentType.toString(),
                HttpHeaders.Accept to contentType.toString(),
            ),
            ObjectParser.toJson(
                mapOf(
                    "identifier" to identifier,
                    "password" to password
                )
            )
        )

        require(response.status.value == 200) { "Failed to create session" }
        return ObjectParser.fromJson(response.bodyAsText())
    }

    suspend fun uploadBlob(accessJwt: String, contentType: ContentType, content: ByteArray): JsonObject {
        val response = HttpRequest().post(
            "$BASE_URL/com.atproto.repo.uploadBlob",
            headers = mapOf(
                HttpHeaders.ContentType to contentType.toString(),
                HttpHeaders.ContentLength to content.size.toString(),
                HttpHeaders.Accept to this.contentType.toString(),
                HttpHeaders.Authorization to "Bearer $accessJwt",
            ),
            body = content,
        )

        require(response.status.value == 200) { "Failed to upload blob" }
        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("blob")
    }

    suspend fun createRecord(
        accessJwt: String,
        did: String,
        text: String,
        images: List<Image> = emptyList()
    ): JsonObject {
        val recordMap = mutableMapOf<String, Any>(
            "text" to text,
            "\$type" to "app.bsky.feed.post",
            "createdAt" to ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).replace("+00:00", "Z"),
        )

        if (images.isNotEmpty()) {
            recordMap["embed"] = mapOf(
                "\$type" to "app.bsky.embed.images",
                "images" to images
            )
        }

        val response = HttpRequest().post(
            "$BASE_URL/com.atproto.repo.createRecord",
            headers = mapOf(
                HttpHeaders.ContentType to contentType.toString(),
                HttpHeaders.Accept to contentType.toString(),
                HttpHeaders.Authorization to "Bearer $accessJwt",
            ),
            body = ObjectParser.toJson(
                mapOf(
                    "repo" to did,
                    "collection" to "app.bsky.feed.post",
                    "record" to recordMap
                )
            )
        )

        require(response.status.value == 200) { "Failed to create record (${response.bodyAsText()})" }
        return ObjectParser.fromJson(response.bodyAsText())
    }
}