package fr.shikkanime.wrappers

import com.google.gson.JsonObject
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.StringUtils
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jsoup.Jsoup
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val TYPE = $$"$type"

object BskyWrapper {
    data class Image(
        val image: JsonObject,
        val alt: String = StringUtils.EMPTY_STRING,
    )

    enum class FacetType(val typeKey: String, val jsonKey: String) {
        LINK("link", "uri"),
        HASHTAG("tag", "tag"),
        ;
    }

    data class Facet(
        val start: Int,
        val end: Int,
        val link: String,
        val type: FacetType
    )

    data class Record(
        val uri: String,
        val cid: String,
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

        require(response.status == HttpStatusCode.OK) { "Failed to create session (${response.status.value} - ${response.bodyAsText()})" }
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

        require(response.status == HttpStatusCode.OK) { "Failed to upload blob (${response.status.value} - ${response.bodyAsText()})" }
        return ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("blob")
    }

    suspend fun createRecord(
        accessJwt: String,
        did: String,
        text: String,
        images: List<Image> = emptyList(),
        replyTo: Record? = null,
        embed: String? = null
    ): Record {
        val (finalText, facets) = getFacets(text)

        val recordMap = mutableMapOf<String, Any>(
            "text" to finalText,
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

        if (facets.isNotEmpty()) {
            recordMap["facets"] = facets.map {
                mapOf(
                    "index" to mapOf(
                        "byteStart" to it.start,
                        "byteEnd" to it.end
                    ),
                    "features" to listOf(
                        mapOf(
                            TYPE to "app.bsky.richtext.facet#${it.type.typeKey}",
                            it.type.jsonKey to it.link
                        )
                    )
                )
            }
        }

        if (replyTo != null) {
            recordMap["reply"] = mapOf(
                "root" to replyTo,
                "parent" to replyTo
            )
        }

        if (embed != null) {
            val (title, description, image) = Jsoup.parse(httpRequest.get(embed).bodyAsText()).run {
                Triple(
                    select("meta[property=og:title]").attr("content"),
                    select("meta[property=og:description]").attr("content"),
                    select("meta[property=og:image]").attr("content")
                )
            }

            val uploadedBlob = HttpRequest.retry(3) {
                uploadBlob(
                    accessJwt,
                    ContentType.Image.JPEG,
                    httpRequest.get(image).readRawBytes()
                )
            }

            recordMap.putIfAbsent("embed", mutableMapOf(
                $$"$type" to "app.bsky.embed.external",
                "external" to mapOf(
                    "uri" to embed,
                    "title" to title,
                    "description" to description,
                    "thumb" to uploadedBlob
                )
            ))
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

        require(response.status == HttpStatusCode.OK) { "Failed to create record (${response.status.value} - ${response.bodyAsText()})" }
        return ObjectParser.fromJson(response.bodyAsText(), Record::class.java)
    }

    fun getFacets(text: String): Pair<String, List<Facet>> {
        val regex = "(https?://[^\\s\\n]+|#[\\wÀ-ÖØ-öø-ÿ]+)".toRegex()
        val facets = mutableListOf<Facet>()
        var finalText = text
        var charOffset = 0

        regex.findAll(text).forEach { match ->
            val value = match.value
            val start = match.range.first
            val currentStart = start + charOffset

            if (value.startsWith("http")) {
                val beautifulLink = value.replace("https?://www\\.|\\?.*".toRegex(), StringUtils.EMPTY_STRING).trim()
                val prefix = finalText.substring(0, currentStart)
                val suffix = finalText.substring(currentStart + value.length)
                
                val byteStart = prefix.toByteArray(Charsets.UTF_8).size
                val byteEnd = byteStart + beautifulLink.toByteArray(Charsets.UTF_8).size
                facets.add(Facet(byteStart, byteEnd, value, FacetType.LINK))
                
                finalText = prefix + beautifulLink + suffix
                charOffset += beautifulLink.length - value.length
            } else if (value.startsWith("#")) {
                val prefix = finalText.substring(0, currentStart)
                val byteStart = prefix.toByteArray(Charsets.UTF_8).size
                val byteEnd = byteStart + value.toByteArray(Charsets.UTF_8).size
                facets.add(Facet(byteStart, byteEnd, value.substring(1), FacetType.HASHTAG))
            }
        }

        return finalText to facets
    }
}