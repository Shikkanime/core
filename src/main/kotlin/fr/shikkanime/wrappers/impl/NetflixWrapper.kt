package fr.shikkanime.wrappers.impl

import com.google.gson.JsonObject
import com.google.inject.Inject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.normalize
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.ZonedDateTime

object NetflixWrapper : AbstractNetflixWrapper() {
    @Inject private lateinit var configCacheService: ConfigCacheService

    private fun extractMaxUrl(json: JsonObject, arrayName: String): String? {
        return json.getAsJsonArray(arrayName)
            .map { it.asJsonObject }
            .maxByOrNull { it.getAsInt("w") ?: 0 }
            ?.getAsString("url")
            ?.substringBefore("?")
    }

    private suspend fun getMetadata(id: Int): ShowMetadata {
        val netflixId = configCacheService.getValueAsString(ConfigPropertyKey.NETFLIX_ID)
        val netflixSecureId = configCacheService.getValueAsString(ConfigPropertyKey.NETFLIX_SECURE_ID)
        require(netflixId?.isNotBlank() == true && netflixSecureId?.isNotBlank() == true) { "NetflixId and NetflixSecureId must be set in the configuration" }

        val response = httpRequest.get(
            "$baseUrl/nq/website/memberapi/release/metadata?movieid=$id&imageFormat=jpg",
            mapOf(
                "Content-Type" to "application/json",
                "Cookie" to "NetflixId=$netflixId; SecureNetflixId=$netflixSecureId"
            )
        )
        require(response.status == HttpStatusCode.OK) { "Failed to get metadata (${response.status.value} - ${response.bodyAsText()})" }
        val metadataJson = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("video")
            ?: throw Exception("Failed to get metadata")

        val episodes = metadataJson.getAsJsonArray("seasons")?.flatMap { seasonJson ->
            seasonJson.asJsonObject.getAsJsonArray("episodes").map { episodeJson ->
                val episode = episodeJson.asJsonObject
                EpisodeMetadata(
                    episode.getAsInt("id") ?: 0,
                    extractMaxUrl(episode, "stills")
                )
            }
        }.orEmpty()

        return ShowMetadata(
            extractMaxUrl(metadataJson, "boxart"),
            extractMaxUrl(metadataJson, "artwork"),
            extractMaxUrl(metadataJson, "storyart"),
            episodes
        )
    }

    override suspend fun getShow(locale: String, id: Int): Show {
        val response = httpRequest.postGraphQL(locale, ObjectParser.toJson(mapOf(
            "operationName" to "DetailModal",
            "variables" to mapOf(
                "opaqueImageFormat" to "PNG",
                "transparentImageFormat" to "PNG",
                "videoMerchEnabled" to true,
                "fetchPromoVideoOverride" to false,
                "hasPromoVideoOverride" to false,
                "promoVideoId" to 0,
                "videoMerchContext" to "BROWSE",
                "isLiveEpisodic" to false,
                "artworkContext" to emptyMap<String, String>(),
                "textEvidenceUiContext" to "ODP",
                "unifiedEntityId" to "Video:$id",
            ),
            "extensions" to mapOf(
                "persistedQuery" to mapOf(
                    "version" to 102,
                    "id" to "ecdd7d08-5135-458c-a111-0e9cb3b6ac21"
                )
            )
        )))
        require(response.status == HttpStatusCode.OK) { "Failed to get show (${response.status.value} - ${response.bodyAsText()})" }
        val showJson = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("data")?.getAsJsonArray("unifiedEntities")?.get(0)?.asJsonObject ?: throw Exception("Failed to get show")
        val metadata = runCatching { getMetadata(id) }.getOrNull()

        return Show(
            id,
            showJson.getAsString("title")!!,
            metadata?.thumbnail,
            metadata?.banner ?: showJson.getAsJsonObject("storyArt")!!.getAsString("url")!!.substringBefore("?"),
            showJson.getAsJsonObject("contextualSynopsis")?.getAsString("text")?.normalize(),
            showJson.getAsJsonObject("seasons")?.getAsInt("totalCount"),
            showJson.getAsString("availabilityStartTime")?.let { ZonedDateTime.parse(it) },
            showJson.getAsInt("runtimeSec")?.toLong(),
            metadata
        )
    }

    override suspend fun getEpisodesByShowId(locale: String, id: Int): List<Episode> {
        val show = getShow(locale, id)

        val seasonsResponse = httpRequest.postGraphQL(locale, ObjectParser.toJson(mapOf(
            "operationName" to "PreviewModalEpisodeSelector",
            "variables" to mapOf(
                "showId" to id,
                "seasonCount" to (show.seasonCount ?: 1),
            ),
            "extensions" to mapOf(
                "persistedQuery" to mapOf(
                    "version" to 102,
                    "id" to "98e53734-bab9-4622-8dbe-b3080c888287"
                )
            )
        )))
        require(seasonsResponse.status == HttpStatusCode.OK) { "Failed to get seasons (${seasonsResponse.status.value} - ${seasonsResponse.bodyAsText()})" }

        val firstVideoObject = ObjectParser.fromJson(seasonsResponse.bodyAsText()).getAsJsonObject("data")
            ?.getAsJsonArray("videos")
            ?.get(0)?.asJsonObject

        // If first video type is movie, return directly
        if (firstVideoObject?.getAsString("__typename") == "Movie") {
            return listOf(
                Episode(
                    show,
                    EncryptionManager.toSHA512("$id-1-1").substring(0..<8),
                    show.id,
                    show.availabilityStartTime,
                    1,
                    1,
                    show.name.normalize(),
                    show.description?.normalize(),
                    "https://www.netflix.com/watch/${show.id}",
                    show.metadata?.cover ?: show.banner.substringBefore("?"),
                    show.runtimeSec!!
                )
            )
        }

        val seasonsJson = firstVideoObject?.getAsJsonObject("seasons")
            ?.getAsJsonArray("edges") ?: throw Exception("Failed to get seasons")

        val seasons = seasonsJson.map { seasonJson ->
            val season = seasonJson.asJsonObject.getAsJsonObject("node")
            Season(
                season.getAsInt("videoId")!!,
                season.getAsString("title")!!,
                season.getAsJsonObject("episodes")!!.getAsInt("totalCount")!!,
            )
        }

        return seasons.flatMapIndexed { index, season ->
            val response = httpRequest.postGraphQL(
                locale, ObjectParser.toJson(
                    mapOf(
                        "operationName" to "PreviewModalEpisodeSelectorSeasonEpisodes",
                        "variables" to mapOf(
                            "seasonId" to season.id,
                            "count" to season.episodeCount,
                            "opaqueImageFormat" to "PNG",
                            "artworkContext" to emptyMap<String, String>(),
                        ),
                        "extensions" to mapOf(
                            "persistedQuery" to mapOf(
                                "version" to 102,
                                "id" to "9492d2b1-888a-47e5-b02d-dbee58872f1e"
                            )
                        )
                    )
                )
            )
            require(response.status == HttpStatusCode.OK) { "Failed to get episodes (${response.status.value} - ${response.bodyAsText()})" }

            val episodesJson = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("data")
                ?.getAsJsonArray("videos")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("episodes")
                ?.getAsJsonArray("edges") ?: throw Exception("Failed to get episodes")

            episodesJson.map { episodeJson ->
                val episode = episodeJson.asJsonObject.getAsJsonObject("node")
                val episodeId = episode.getAsInt("videoId")!!

                Episode(
                    show,
                    EncryptionManager.toSHA512("${show.id}-${index + 1}-${episode.getAsInt("number")!!}").substring(0..<8),
                    episodeId,
                    episode.getAsString("availabilityStartTime")?.let { ZonedDateTime.parse(it) },
                    index + 1,
                    episode.getAsInt("number")!!,
                    episode.getAsString("title")?.normalize(),
                    episode.getAsJsonObject("contextualSynopsis")?.getAsString("text")?.normalize(),
                    "https://www.netflix.com/watch/$episodeId",
                    show.metadata?.episodes?.find { it.id == episodeId }?.image ?: episode.getAsJsonObject("artwork")!!.getAsString("url")!!.substringBefore("?"),
                    episode.getAsInt("runtimeSec")!!.toLong()
                )
            }
        }
    }
}