package fr.shikkanime.admin.dtos.episode

import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.EpisodeWithChildren
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * Administration view of one episode in the release list.
 *
 * The row carries the anime the episode belongs to, because the list spans the whole
 * catalog: the normalized title of the anime is what identifies it, and [title] is the main title
 * of the episode. Releases, full descriptions and artwork are loaded with [AdminEpisodeDetailDto]
 * when the administrator opens the episode.
 */
@Serializable
data class AdminLatestEpisodeDto(
    val id: Uuid,
    val animeId: Uuid,
    val animeTitle: String,
    val season: Int,
    val episodeType: EpisodeType,
    val number: Int,
    val releaseDateTime: LocalDateTime,
    val title: String?,
    val releaseCount: Int,
    val localizationCount: Int
)

/**
 * Maps a stored episode aggregate to its release list view.
 *
 * The displayed title is the first French localization, falling back to the first available
 * translation, and stays `null` when the episode has no localized text - the client then displays
 * the identity of the episode.
 *
 * @receiver stored episode with the rows it owns.
 * @param animeTitle normalized title of the anime owning the episode.
 * @return the administration view of this release.
 * @see [AdminLatestEpisodeDto]
 * @see [EpisodeWithChildren]
 */
fun EpisodeWithChildren.toAdminLatestDto(animeTitle: String): AdminLatestEpisodeDto =
    AdminLatestEpisodeDto(
        id = requireNotNull(episode.id),
        animeId = requireNotNull(episode.animeId),
        animeTitle = animeTitle,
        season = episode.season,
        episodeType = episode.episodeType,
        number = episode.number,
        releaseDateTime = episode.releaseDateTime,
        title = localizations.firstOrNull { it.locale.name.startsWith("FR") }?.title
            ?: localizations.firstOrNull()?.title,
        releaseCount = releases.size,
        localizationCount = localizations.size
    )
