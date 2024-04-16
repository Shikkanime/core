package fr.shikkanime.wrappers

import com.google.gson.JsonObject
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val TYPE = "\$type"

object BskyWrapper {
    data class Image(
        val image: JsonObject,
        val alt: String = "",
    )

    data class Facet(
        val link: String,
        val start: Int,
        val end: Int,
    )

    private const val BASE_URL = "https://bsky.social/xrpc"
    private val contentType = ContentType.Application.Json
    private val httpRequest = HttpRequest()

    suspend fun createSession(identifier: String, password: String): JsonObject {
        val response = httpRequest.post(
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
        val response = httpRequest.post(
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
        images: List<Image> = emptyList(),
    ): JsonObject {
        val recordMap = mutableMapOf<String, Any>(
            "text" to text,
            TYPE to "app.bsky.feed.post",
            "createdAt" to ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).replace("+00:00", "Z"),
        )

        if (images.isNotEmpty()) {
            recordMap["embed"] = mapOf(
                TYPE to "app.bsky.embed.images",
                "images" to images
            )
        }

        // Get all links in the text, and their start and end indexes
        val facets = text.split(" ").mapNotNull { word ->
            val link = word.trim()
            if (link.startsWith("http")) {
                val start = text.indexOf(link)
                val end = start + link.length
                Facet(link, start, end)
            } else null
        }

        if (facets.isNotEmpty()) {
            recordMap["facets"] = facets.map {
                mapOf(
                    "index" to mapOf(
                        "byteStart" to it.start,
                        "byteEnd" to it.end
                    ),
                    "features" to listOf(
                        mapOf(
                            TYPE to "app.bsky.richtext.facet#link",
                            "uri" to it.link
                        )
                    )
                )
            }
        }

        val response = httpRequest.post(
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