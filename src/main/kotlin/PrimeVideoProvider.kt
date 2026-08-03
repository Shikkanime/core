package fr.shikkanime

import fr.shikkanime.models.*
import java.time.LocalDate

class PrimeVideoProvider(val primeVideoWrapper: PrimeVideoWrapper) {
    data class Anime(
        val id: String,
        val name: String,
        val description: String?,
        val attachments: Map<String, String>,
    )

    data class Episode(
        val id: String,
        val releaseDate: LocalDate,
        val season: Int,
        val number: Int,
        val title: String,
        val description: String?,
        val url: String,
        val image: String,
        val duration: Long,
        val audioLocales: Set<String>,
        val subtitleLocales: Set<String>,
    )

    private fun isAvailableEpisode(detail: PrimeVideoBtfStateDetailDetail, metadata: PrimeVideoBtfStateMetadata): Boolean =
        detail.titleType == PrimeVideoBtfStateDetailDetailTitleType.EPISODE
                && (metadata.traits.isNullOrEmpty() || metadata.upcomingSynopsis.isNullOrBlank())

    private fun toEpisode(season: PrimeVideoAtfStateSeason, id: String, detail: PrimeVideoBtfStateDetailDetail, url: String): Episode =
        Episode(
            id,
            detail.releaseDate,
            season.sequenceNumber,
            requireNotNull(detail.episodeNumber) { "Episode number is null" },
            detail.title,
            detail.synopsis,
            url,
            detail.images.covershot,
            requireNotNull(detail.duration) { "Episode duration is null" },
            detail.audioTracks,
            detail.subtitles
        )

    private fun getEpisodesFromState(season: PrimeVideoAtfStateSeason, state: PrimeVideoBtfState): Set<Episode> =
        state.detail.detail.entries.mapNotNull { (episodeId, detail) ->
            val metadata = state.metadata[episodeId] ?: return@mapNotNull null
            val url = state.self[episodeId]?.link ?: return@mapNotNull null
            if (!isAvailableEpisode(detail, metadata)) return@mapNotNull null

            toEpisode(season, episodeId, detail, url)
        }.toSet()

    private suspend fun getSeasonState(
        locale: String,
        season: PrimeVideoAtfStateSeason,
        initialState: PrimeVideoBtfState,
        pageTitleId: String,
    ): PrimeVideoBtfState =
        if (season.seasonId == pageTitleId)
            initialState
        else
            primeVideoWrapper.fetchDetail(locale, season.seasonLink).body.btf.state

    private suspend fun getPaginatedEpisodes(
        season: PrimeVideoAtfStateSeason,
        state: PrimeVideoBtfState,
    ): List<Episode> {
        val episodes = mutableListOf<Episode>()
        var nextPageToken = state.episodeList.nextPageToken

        while (nextPageToken != null) {
            val widgets = primeVideoWrapper.getDetailWidgets(season.seasonId, nextPageToken).widgets

            widgets.episodeList.episodes
                ?.filter { episode -> episode.detail.titleType == PrimeVideoBtfStateDetailDetailTitleType.EPISODE }
                ?.mapTo(episodes) { episode ->
                    toEpisode(
                        season = season,
                        id = requireNotNull(episode.detail.catalogId) { "Episode catalogId is null" },
                        detail = episode.detail,
                        url = episode.self.link,
                    )
                }

            nextPageToken = widgets.episodeList.nextPageToken
        }

        return episodes
    }

    suspend fun getEpisodes(locale: String, id: String): Set<Episode> {
        val detail = primeVideoWrapper.fetchDetailWithId(locale, id)
        val pageTitleId = detail.body.atf.state.pageTitleId
        val headerDetail = detail.body.atf.state.detail.headerDetail[pageTitleId] ?: return emptySet()

        val anime = Anime(
            id,
            headerDetail.parentTitle,
            headerDetail.synopsis,
            mapOf(
                "BANNER" to headerDetail.images.covershot,
                "CAROUSEL" to headerDetail.images.heroshot,
                "TITLE" to headerDetail.images.titleLogo
            )
        )

        return detail.body.atf.state.seasons[pageTitleId]?.flatMap { season ->
            val seasonState = getSeasonState(
                locale = locale,
                season = season,
                initialState = detail.body.btf.state,
                pageTitleId = pageTitleId,
            )

            println(seasonState)
            getEpisodesFromState(season, seasonState) + getPaginatedEpisodes(season, seasonState)
        }?.toSet().orEmpty()
    }
}