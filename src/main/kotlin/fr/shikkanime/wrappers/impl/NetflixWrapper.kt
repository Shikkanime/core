package fr.shikkanime.wrappers.impl

import com.google.gson.JsonObject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsBoolean
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.normalize
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.ZonedDateTime

object NetflixWrapper : AbstractNetflixWrapper() {
    data class NetflixAuthentification(
        val id: String,
        val secureId: String,
        val authUrl: String
    )

    private fun getNetflixAuthentificationFromConfig(): NetflixAuthentification {
        val configCacheService = Constant.injector.getInstance(ConfigCacheService::class.java)
        val netflixId = configCacheService.getValueAsString(ConfigPropertyKey.NETFLIX_ID)
        val netflixSecureId = configCacheService.getValueAsString(ConfigPropertyKey.NETFLIX_SECURE_ID)
        val netflixAuthUrl = configCacheService.getValueAsString(ConfigPropertyKey.NETFLIX_AUTH_URL)
        require(netflixId?.isNotBlank() == true && netflixSecureId?.isNotBlank() == true && netflixAuthUrl?.isNotBlank() == true) { "NetflixId, NetflixSecureId and NetflixAuthUrl must be set in the configuration" }
        return NetflixAuthentification(netflixId, netflixSecureId, netflixAuthUrl)
    }

    private fun extractMaxUrl(json: JsonObject, arrayName: String): String? {
        return json.getAsJsonArray(arrayName)
            .map { it.asJsonObject }
            .maxByOrNull { it.getAsInt("w") ?: 0 }
            ?.getAsString("url")
            ?.substringBefore("?")
    }

    private suspend fun getMetadata(id: Int): ShowMetadata {
        val netflixAuthentification = getNetflixAuthentificationFromConfig()

        val response = httpRequest.get(
            "$baseUrl/nq/website/memberapi/release/metadata?movieid=$id&imageFormat=jpg",
            mapOf(
                "Content-Type" to "application/json",
                "Cookie" to "NetflixId=${netflixAuthentification.id}; SecureNetflixId=${netflixAuthentification.secureId}"
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
            metadata?.banner ?: showJson.getAsJsonObject("boxartHighRes")!!.getAsString("url")!!.substringBefore("?"),
            metadata?.carousel ?: showJson.getAsJsonObject("storyArt")!!.getAsString("url")!!.substringBefore("?"),
            showJson.getAsJsonObject("contextualSynopsis")?.getAsString("text")?.normalize(),
            showJson.getAsJsonObject("seasons")?.getAsInt("totalCount"),
            showJson.getAsString("availabilityStartTime")?.let { ZonedDateTime.parse(it) },
            showJson.getAsBoolean("isAvailable") ?: false,
            showJson.getAsBoolean("isPlayable") ?: false,
            showJson.getAsInt("runtimeSec")?.toLong(),
            metadata
        )
    }

    override suspend fun getEpisodesByShowId(locale: String, id: Int): Array<Episode> {
        val show = getShow(locale, id)
        val seasonsResponse = fetchSeasonsData(locale, id, show.seasonCount ?: 1)
        val firstVideoObject = parseFirstVideoObject(seasonsResponse)
        
        return if (firstVideoObject?.getAsString("__typename") == "Movie") {
            createMovieEpisode(show, id)
        } else {
            createSeriesEpisodes(locale, show, firstVideoObject)
        }.toTypedArray()
    }

    private suspend fun fetchSeasonsData(locale: String, id: Int, seasonCount: Int): HttpResponse {
        val seasonsResponse = httpRequest.postGraphQL(locale, ObjectParser.toJson(mapOf(
            "operationName" to "PreviewModalEpisodeSelector",
            "variables" to mapOf(
                "showId" to id,
                "seasonCount" to seasonCount
            ),
            "extensions" to mapOf(
                "persistedQuery" to mapOf(
                    "version" to 102,
                    "id" to "98e53734-bab9-4622-8dbe-b3080c888287"
                )
            )
        )))
        require(seasonsResponse.status == HttpStatusCode.OK) { 
            "Failed to get seasons (${seasonsResponse.status.value} - ${seasonsResponse.bodyAsText()})" 
        }
        return seasonsResponse
    }

    private suspend fun parseFirstVideoObject(seasonsResponse: HttpResponse): JsonObject? {
        return ObjectParser.fromJson(seasonsResponse.bodyAsText())
            .getAsJsonObject("data")
            ?.getAsJsonArray("videos")
            ?.firstOrNull()?.asJsonObject
    }

    private suspend fun createMovieEpisode(show: Show, id: Int): List<Episode> {
        val releaseDateTime = show.availabilityStartTime
        val isAvailable = show.isAvailable
        val isPlayable = show.isPlayable

        if (!(isAvailable && isPlayable)) {
            return emptyList()
        }

        return listOf(
            Episode(
                show,
                EncryptionManager.toSHA512("$id-1-1").take(8),
                show.id,
                releaseDateTime,
                1,
                1,
                show.name.normalize(),
                show.description?.normalize(),
                "$baseUrl/watch/${show.id}",
                show.metadata?.carousel ?: show.banner.substringBefore("?"),
                show.runtimeSec!!,
                runCatching { getEpisodeAudioTrackList(show.id) }.getOrNull() ?: emptySet()
            )
        )
    }

    private suspend fun createSeriesEpisodes(locale: String, show: Show, firstVideoObject: JsonObject?): List<Episode> {
        val seasonsJson = firstVideoObject?.getAsJsonObject("seasons")
            ?.getAsJsonArray("edges") ?: throw Exception("Failed to get seasons")
        
        val seasons = parseSeasons(seasonsJson)
        
        return seasons.flatMapIndexed { index, season ->
            fetchAndCreateEpisodesForSeason(locale, show, season, index + 1)
        }
    }

    private fun parseSeasons(seasonsJson: com.google.gson.JsonArray): List<Season> {
        return seasonsJson.map { seasonJson ->
            val season = seasonJson.asJsonObject.getAsJsonObject("node")
            Season(
                season.getAsInt("videoId")!!,
                season.getAsString("title")!!,
                season.getAsJsonObject("episodes").getAsInt("totalCount")!!
            )
        }
    }

    private suspend fun fetchAndCreateEpisodesForSeason(locale: String, show: Show, season: Season, seasonNumber: Int): List<Episode> {
        val response = httpRequest.postGraphQL(locale, ObjectParser.toJson(mapOf(
            "operationName" to "PreviewModalEpisodeSelectorSeasonEpisodes",
            "variables" to mapOf(
                "seasonId" to season.id,
                "count" to season.episodeCount,
                "opaqueImageFormat" to "PNG",
                "artworkContext" to emptyMap<String, String>()
            ),
            "extensions" to mapOf(
                "persistedQuery" to mapOf(
                    "version" to 102,
                    "id" to "9492d2b1-888a-47e5-b02d-dbee58872f1e"
                )
            )
        )))
        require(response.status == HttpStatusCode.OK) { 
            "Failed to get episodes (${response.status.value} - ${response.bodyAsText()})" 
        }

        val episodesJson = ObjectParser.fromJson(response.bodyAsText())
            .getAsJsonObject("data")
            ?.getAsJsonArray("videos")
            ?.firstOrNull()?.asJsonObject
            ?.getAsJsonObject("episodes")
            ?.getAsJsonArray("edges") ?: throw Exception("Failed to get episodes")

        return episodesJson.mapNotNull { episodeJson ->
            createEpisodeFromJson(show, episodeJson.asJsonObject, seasonNumber)
        }
    }

    private suspend fun createEpisodeFromJson(show: Show, episodeJson: JsonObject, seasonNumber: Int): Episode? {
        val episode = episodeJson.getAsJsonObject("node")
        val episodeId = episode.getAsInt("videoId")!!
        val episodeNumber = episode.getAsInt("number")!!
        val releaseDateTime = episode.getAsString("availabilityStartTime")?.let { ZonedDateTime.parse(it) }
        val isAvailable = episode.getAsBoolean("isAvailable") ?: false
        val isPlayable = episode.getAsBoolean("isPlayable") ?: false

        if (!(isAvailable && isPlayable)) {
            return null
        }

        return Episode(
            show,
            EncryptionManager.toSHA512("${show.id}-$seasonNumber-$episodeNumber").take(8),
            episodeId,
            releaseDateTime,
            seasonNumber,
            episodeNumber,
            episode.getAsString("title")?.normalize(),
            episode.getAsJsonObject("contextualSynopsis")?.getAsString("text")?.normalize(),
            "$baseUrl/watch/$episodeId",
            show.metadata?.episodes?.find { it.id == episodeId }?.image
                ?: episode.getAsJsonObject("artwork").getAsString("url")!!.substringBefore("?"),
            episode.getAsInt("runtimeSec")!!.toLong(),
            runCatching { getEpisodeAudioTrackList(episodeId) }.getOrNull() ?: emptySet()
        )
    }

    suspend fun getEpisodeAudioTrackList(id: Int): Set<String> {
        val netflixAuthentification = getNetflixAuthentificationFromConfig()

        val response = httpRequest.post(
            "$baseUrl/nq/website/memberapi/release/pathEvaluator?method=call&original_path=%2Fshakti%2Fmre%2FpathEvaluator",
            mapOf("Cookie" to "NetflixId=${netflixAuthentification.id}; SecureNetflixId=${netflixAuthentification.secureId}"),
            FormDataContent(parametersOf(
                "callPath" to listOf("[\"videos\",$id,\"audio\"]"),
                "authURL" to listOf(netflixAuthentification.authUrl)
            ))
        )
        require(response.status == HttpStatusCode.OK) { "Failed to get audio tracks (${response.status.value} - ${response.bodyAsText()})" }
        val audioTracks = ObjectParser.fromJson(response.bodyAsText()).getAsJsonObject("jsonGraph")
            ?.getAsJsonObject("videos")
            ?.getAsJsonObject(id.toString())
            ?.getAsJsonObject("audio")
            ?.getAsJsonArray("value")
            ?.map { it.asJsonObject.getAsString("isoCode") }
            ?: throw Exception("Failed to get metadata")

        return buildSet {
            if ("ja-jpn" in audioTracks) add("ja-JP")
            if ("ja-jpn" !in audioTracks && "en-eng" in audioTracks) add("en-US")
            if ("fr-fra" in audioTracks) add("fr-FR")
        }
    }
}