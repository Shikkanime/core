package fr.shikkanime.admin.dtos.episode

import fr.shikkanime.admin.dtos.image.AdminImageDto
import fr.shikkanime.admin.dtos.image.toAdminDto as toImageAdminDto
import fr.shikkanime.admin.dtos.localization.AdminLocalizationDto
import fr.shikkanime.admin.dtos.localization.toAdminDto as toLocalizationAdminDto
import fr.shikkanime.models.EpisodeLocalization
import fr.shikkanime.models.EpisodeRelease
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.EpisodeWithChildren
import fr.shikkanime.models.Image
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * Full administration view of one episode.
 *
 * The detail screen shows everything the database stores about the episode: its identity, its
 * audit columns, its localized texts with their full descriptions, every platform release with its
 * URL and audio locale, and its artwork URLs. The episode list uses [AdminEpisodeSummaryDto]
 * instead, so one page of an anime does not carry every release and description.
 */
@Serializable
data class AdminEpisodeDetailDto(
    val id: Uuid,
    val animeId: Uuid?,
    val season: Int,
    val episodeType: EpisodeType,
    val number: Int,
    val releaseDateTime: LocalDateTime,
    val duration: Long,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val localizations: List<AdminLocalizationDto>,
    val releases: List<AdminReleaseDto>,
    val images: List<AdminImageDto>
)

/**
 * Maps a stored episode aggregate to its full administration view.
 *
 * @receiver stored episode with the rows it owns.
 * @return the full administration view of this episode.
 * @see [AdminEpisodeDetailDto]
 * @see [EpisodeWithChildren]
 */
fun EpisodeWithChildren.toAdminDetailDto(): AdminEpisodeDetailDto =
    AdminEpisodeDetailDto(
        id = requireNotNull(episode.id),
        animeId = episode.animeId,
        season = episode.season,
        episodeType = episode.episodeType,
        number = episode.number,
        releaseDateTime = episode.releaseDateTime,
        duration = episode.duration,
        createdAt = episode.createdAt,
        updatedAt = episode.updatedAt,
        localizations = localizations.map(EpisodeLocalization::toLocalizationAdminDto),
        releases = releases.map(EpisodeRelease::toAdminDto),
        images = images.map(Image::toImageAdminDto)
    )
