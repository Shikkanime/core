package fr.shikkanime.wrappers.impl

import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Locale
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.dtos.*
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.Duration

object NetflixWrapper : AbstractNetflixWrapper() {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val configCacheService by lazy { Constant.injector.getInstance(ConfigCacheService::class.java) }

    private suspend fun getNetflixAuthentificationFromConfig() = MapCache.getOrComputeAsync(
        "NetflixWrapper.getNetflixAuthentificationFromConfig",
        typeToken = object : TypeToken<MapCacheValue<NetflixAuthentification>>() {},
        duration = Duration.ofHours(1),
        classes = listOf(Config::class.java),
        key = StringUtils.EMPTY_STRING
    ) {
        val netflixId = configCacheService.getValueAsString(ConfigPropertyKey.NETFLIX_ID)
        val netflixSecureId = configCacheService.getValueAsString(ConfigPropertyKey.NETFLIX_SECURE_ID)
        require(netflixId?.isNotBlank() == true && netflixSecureId?.isNotBlank() == true) { "NetflixId and NetflixSecureId must be set in the configuration" }
        val authUrl = extractAuthUrl(HttpRequest.get(baseUrl, mapOf(HttpHeaders.Cookie to getCookieValue(netflixId, netflixSecureId))).bodyAsText())
        NetflixAuthentification(netflixId, netflixSecureId, authUrl)
    }

    private suspend fun getMetadata(id: Int): ShowMetadata {
        val netflixAuthentification = getNetflixAuthentificationFromConfig()

        val response = HttpRequest.get(
            "$baseUrl/nq/website/memberapi/release/metadata?movieid=$id&imageFormat=jpg",
            mapOf(HttpHeaders.Cookie to getCookieValue(netflixAuthentification.id, netflixAuthentification.secureId))
        )
        require(response.status == HttpStatusCode.OK) { "Failed to get metadata (${response.status.value} - ${response.bodyAsText()})" }
        val metadataVideo = response.body<NetflixMetadataResponse>().video

        return ShowMetadata(
            metadataVideo.boxart.maxByWidthCleanUrl(),
            metadataVideo.artwork.maxByWidthCleanUrl(),
            metadataVideo.storyart.maxByWidthCleanUrl(),
            metadataVideo.seasons.flatMap { season ->
                season.episodes.map { episode ->
                    EpisodeMetadata(
                        episode.id,
                        episode.images.maxByWidthCleanUrl()
                    )
                }
            }
        )
    }

    override suspend fun getLatestShows(): Array<LatestShow> {
        val netflixAuthentification = getNetflixAuthentificationFromConfig()
        val response = HttpRequest.post(
            "$baseUrl/nq/website/memberapi/release/pathEvaluator?isTop10Supported=true&original_path=%2Fshakti%2Fmre%2FpathEvaluator",
            mapOf(HttpHeaders.Cookie to getCookieValue(netflixAuthentification.id, netflixAuthentification.secureId)),
            FormDataContent(
                parametersOf(
                    "path" to listOf("[\"lolomoByCategory\",\"comingSoon\",[0,1,2,3,4,5],{\"from\":0,\"to\":10},\"itemSummary\"]"),
                    "authURL" to listOf(netflixAuthentification.authUrl),
                )
            )
        )
        require(response.status == HttpStatusCode.OK) { "Failed to get latest shows (${response.status.value} - ${response.bodyAsText()})" }
        val latestShows = response.body<NetflixLatestShowsResponse>().latestShows()

        return latestShows.map { valueObject ->
            LatestShow(
                id = requireNotNull(valueObject.id) { "Missing id for latest show in list" },
                title = requireNotNull(valueObject.title) { "Missing title for latest show in list" },
                isPlayable = valueObject.availability?.isPlayable ?: false,
            )
        }.distinctBy { it.id }.toTypedArray()
    }

    override suspend fun getShow(locale: String, id: Int): Show {
        val response = HttpRequest.postGraphQL(locale, ObjectParser.toJson(mapOf(
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
        val unifiedEntity = response.body<NetflixResponse<NetflixDetailModalData>>().data.unifiedEntities.firstOrNull() ?: throw Exception("Failed to get unified entity for show $id")
        val metadata = if (unifiedEntity.isAvailable) runCatching { getMetadata(id) }.getOrNull() else null

        return Show(
            unifiedEntity.parentShow?.id ?: id,
            unifiedEntity.title,
            metadata?.thumbnail,
            metadata?.banner ?: unifiedEntity.boxartHighRes.cleanUrl(),
            metadata?.carousel ?: unifiedEntity.storyArt.cleanUrl(),
            unifiedEntity.logoArtwork?.cleanUrl(),
            unifiedEntity.contextualSynopsis?.text,
            unifiedEntity.seasons?.totalCount,
            unifiedEntity.availabilityStartTime,
            unifiedEntity.isAvailable,
            unifiedEntity.isPlayable,
            unifiedEntity.genreTags.edges.map { it.node.name },
            unifiedEntity.runtimeSec,
            metadata
        )
    }

    override suspend fun getEpisodesByShowId(locale: String, showId: Int): Array<Episode> {
        val show = getShow(locale, showId)
        val previewModalEpisodeSelector = fetchSeasonsData(locale, showId, show.seasonCount ?: 1).firstOrNull()

        return when (previewModalEpisodeSelector?.type) {
            "Season" -> getEpisodesByShowId(locale, show.id).toList()
            "Movie" -> createMovieEpisode(locale, show)
            else -> createSeriesEpisodes(locale, show, previewModalEpisodeSelector)
        }.toTypedArray()
    }

    private suspend fun fetchSeasonsData(locale: String, id: Int, seasonCount: Int): List<NetflixPreviewModalEpisodeSelectorVideo> {
        val response = HttpRequest.postGraphQL(locale, ObjectParser.toJson(mapOf(
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
        require(response.status == HttpStatusCode.OK) { "Failed to get seasons (${response.status.value} - ${response.bodyAsText()})" }
        return response.body<NetflixResponse<NetflixVideos<NetflixPreviewModalEpisodeSelectorVideo>>>().data.videos
    }

    private suspend fun createMovieEpisode(locale: String, show: Show): List<Episode> {
        val releaseDateTime = show.availabilityStartTime
        val isAvailable = show.isAvailable
        val isPlayable = show.isPlayable

        if (!(isAvailable && isPlayable)) {
            return emptyList()
        }

        return listOf(
            Episode(
                show,
                show.id,
                releaseDateTime,
                1,
                EpisodeType.FILM,
                1,
                show.name,
                show.description,
                "$baseUrl/watch/${show.id}",
                show.metadata?.carousel ?: show.banner,
                show.runtimeSec!!,
                runCatching { getEpisodeAudioTrackList(locale, show.id) }
                    .map { it[show.id] ?: setOf(Locale.JA_JP.code) }
                    .onFailure { logger.warning("Failed to get audio tracks for movie ${show.id}: ${it.message}") }
                    .getOrNull() ?: setOf(Locale.JA_JP.code)
            )
        )
    }

    private suspend fun createSeriesEpisodes(locale: String, show: Show, previewModalEpisodeSelector: NetflixPreviewModalEpisodeSelectorVideo?): List<Episode> {
        val seasons = previewModalEpisodeSelector?.seasons
            ?.edges
            ?.map { it.node } ?: throw Exception("Failed to get seasons")
        
        return seasons.flatMapIndexed { index, season ->
            fetchAndCreateEpisodesForSeason(locale, show, season, index + 1)
        }
    }

    private suspend fun fetchAndCreateEpisodesForSeason(locale: String, show: Show, season: NetflixSeason, seasonNumber: Int): List<Episode> {
        val response = HttpRequest.postGraphQL(locale, ObjectParser.toJson(mapOf(
            "operationName" to "PreviewModalEpisodeSelectorSeasonEpisodes",
            "variables" to mapOf(
                "seasonId" to season.id,
                "count" to season.episodes.totalCount,
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
        require(response.status == HttpStatusCode.OK) { "Failed to get episodes (${response.status.value} - ${response.bodyAsText()})" }

        val episodes = response.body<NetflixResponse<NetflixVideos<NetflixPreviewModalEpisodeSelectorSeasonEpisodes>>>().data
            .videos
            .firstOrNull()
            ?.episodes
            ?.edges
            ?.map { it.node } ?: throw Exception("Failed to get episodes")

        val episodeIds = episodes.filter { it.isAvailable && it.isPlayable }
            .map { it.id }
            .toIntArray()

        val audioTracksMap = if (episodeIds.isNotEmpty())
            runCatching { getEpisodeAudioTrackList(locale, *episodeIds) }
                .onFailure { logger.warning("Failed to get audio tracks for season ${season.id}: ${it.message}") }
                .getOrNull() ?: emptyMap()
        else emptyMap()

        return episodes.mapNotNull { episode ->
            createEpisodeFromJson(show, episode, seasonNumber, audioTracksMap)
        }
    }

    private fun createEpisodeFromJson(
        show: Show,
        episode: NetflixEpisode,
        seasonNumber: Int,
        audioTracksMap: Map<Int, Set<String>>
    ): Episode? {
        if (!(episode.isAvailable && episode.isPlayable)) {
            return null
        }

        return Episode(
            show,
            episode.id,
            episode.availabilityStartTime,
            seasonNumber,
            EpisodeType.EPISODE,
            episode.number,
            episode.title,
            episode.description?.text,
            "$baseUrl/watch/${episode.id}",
            show.metadata?.episodes?.find { it.id == episode.id }?.image ?: episode.artwork.cleanUrl(),
            episode.runtimeSec,
            audioTracksMap[episode.id] ?: setOf(Locale.JA_JP.code)
        )
    }

    suspend fun getEpisodeAudioTrackList(locale: String, vararg ids: Int): Map<Int, Set<String>> {
        val countryCode = requireNotNull(CountryCode.fromLocale(locale)) { "Unsupported locale: $locale" }
        val netflixAuthentification = getNetflixAuthentificationFromConfig()

        return ShikkanimeWorkerWrapper.getNetflixEpisodes(
            netflixAuthentification.id,
            netflixAuthentification.secureId,
            *ids
        ).associate { it.id to LocaleUtils.getAllowedLocales(countryCode, it.audioLocales) }
            .filterValues { it.isNotEmpty() }
    }
}