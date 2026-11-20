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
 * An episode of the release list paired with the normalized title of its anime.
 *
 * The title comes from another aggregate than the episode, so the mapping receives it composed
 * here instead of through a parameter.
 *
 * @property episode stored episode with the rows it owns.
 * @property animeTitle normalized title of the anime owning the episode.
 * @see [AdminLatestEpisodeDto]
 */
data class EpisodeWithAnimeTitle(
    val episode: EpisodeWithChildren,
    val animeTitle: String
)

/**
 * Maps a stored episode and its anime title to the release list view.
 *
 * The displayed title is the first French localization, falling back to the first available
 * translation, and stays `null` when the episode has no localized text - the client then displays
 * the identity of the episode.
 *
 * @receiver stored episode with its rows and the title of its anime.
 * @return the administration view of this release.
 * @see [AdminLatestEpisodeDto]
 * @see [EpisodeWithAnimeTitle]
 */
fun EpisodeWithAnimeTitle.toAdminLatestDto(): AdminLatestEpisodeDto =
    AdminLatestEpisodeDto(
        id = requireNotNull(episode.id),
        animeId = requireNotNull(episode.animeId),
        animeTitle = animeTitle,
        season = episode.season,
        episodeType = episode.episodeType,
        number = episode.number,
        releaseDateTime = episode.releaseDateTime,
        title = episode.localizations.firstOrNull { it.locale.name.startsWith("FR") }?.title
            ?: episode.localizations.firstOrNull()?.title,
        releaseCount = episode.releases.size,
        localizationCount = episode.localizations.size
    )
