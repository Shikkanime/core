package fr.shikkanime.admin.dtos.episode

import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.EpisodeWithChildren
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * Compact administration view of one episode in the episode list of an anime.
 *
 * The payload only carries what a list row displays: the episode identity, its broadcast date and
 * duration, its display title and the number of releases and translations it owns. Releases, full
 * descriptions and artwork are loaded with [AdminEpisodeDetailDto] when the administrator opens
 * the episode.
 */
@Serializable
data class AdminEpisodeSummaryDto(
    val id: Uuid,
    val season: Int,
    val episodeType: EpisodeType,
    val number: Int,
    val releaseDateTime: LocalDateTime,
    val duration: Long,
    val title: String?,
    val releaseCount: Int,
    val localizationCount: Int
)

/**
 * Maps a stored episode aggregate to its compact list view.
 *
 * The displayed title is the first French localization, falling back to the first available
 * translation, and stays `null` when the episode has no localized text.
 *
 * @receiver stored episode with the rows it owns.
 * @return the compact administration view of this episode.
 * @see [AdminEpisodeSummaryDto]
 * @see [EpisodeWithChildren]
 */
fun EpisodeWithChildren.toAdminSummaryDto(): AdminEpisodeSummaryDto =
    AdminEpisodeSummaryDto(
        id = requireNotNull(episode.id),
        season = episode.season,
        episodeType = episode.episodeType,
        number = episode.number,
        releaseDateTime = episode.releaseDateTime,
        duration = episode.duration,
        title = localizations.firstOrNull { it.locale.name.startsWith("FR") }?.title
            ?: localizations.firstOrNull()?.title,
        releaseCount = releases.size,
        localizationCount = localizations.size
    )
