package fr.shikkanime.wrappers.impl

import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.normalize
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.ZonedDateTime

object NetflixWrapper : AbstractNetflixWrapper(){
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

        return Show(
            id,
            showJson.getAsString("title")!!,
            showJson.getAsJsonObject("storyArt")!!.getAsString("url")!!.substringBefore("?"),
            showJson.getAsJsonObject("contextualSynopsis")?.getAsString("text")?.normalize(),
            showJson.getAsJsonObject("seasons")?.getAsInt("totalCount"),
            showJson.getAsString("availabilityStartTime")?.let { ZonedDateTime.parse(it) },
            showJson.getAsInt("runtimeSec")?.toLong()
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
                    show.banner.substringBefore("?"),
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
            val response = httpRequest.postGraphQL(locale, ObjectParser.toJson(mapOf(
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
            )))
            require(response.status == HttpStatusCode.OK) { "Failed to get episodes (${response.status.value} - ${response.bodyAsText()})" }

            val episodesJson = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("data")
                ?.getAsJsonArray("videos")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("episodes")
                ?.getAsJsonArray("edges") ?: throw Exception("Failed to get episodes")

            episodesJson.map { episodeJson ->
                val episode = episodeJson.asJsonObject.getAsJsonObject("node")

                Episode(
                    show,
                    EncryptionManager.toSHA512("$id-${index + 1}-${episode.getAsInt("number")!!}").substring(0..<8),
                    episode.getAsInt("videoId")!!,
                    episode.getAsString("availabilityStartTime")?.let { ZonedDateTime.parse(it) },
                    index + 1,
                    episode.getAsInt("number")!!,
                    episode.getAsString("title")?.normalize(),
                    episode.getAsJsonObject("contextualSynopsis")?.getAsString("text")?.normalize(),
                    "https://www.netflix.com/watch/${episode.getAsInt("videoId")!!}",
                    episode.getAsJsonObject("artwork")!!.getAsString("url")!!.substringBefore("?"),
                    episode.getAsInt("runtimeSec")!!.toLong()
                )
            }
        }
    }
}