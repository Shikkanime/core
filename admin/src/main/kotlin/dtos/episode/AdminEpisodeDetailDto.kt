package fr.shikkanime.admin.dtos.episode

import fr.shikkanime.admin.dtos.image.AdminImageDto
import fr.shikkanime.admin.dtos.localization.AdminLocalizationDto
import fr.shikkanime.models.*
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid
import fr.shikkanime.admin.dtos.image.toAdminDto as toImageAdminDto
import fr.shikkanime.admin.dtos.localization.toAdminDto as toLocalizationAdminDto

/**
 * Full administration view of one episode.
 *
 * The detail screen shows everything the database stores about the episode: its identity, its
 * audit columns, its localized texts with their full descriptions, every platform release with its
 * URL and audio locale, its metadata platform links and its artwork URLs. The episode list uses
 * [AdminEpisodeSummaryDto] instead, so one page of an anime does not carry every release and
 * description.
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
    val images: List<AdminImageDto>,
    val metadataLinks: List<AdminEpisodeMetadataLinkDto>
)

/**
 * An episode aggregate paired with its metadata platform links.
 *
 * The detail view joins two aggregates the persistence keeps apart, so the mapping receives
 * them composed here instead of through a parameter.
 *
 * @property episode stored episode with the rows it owns.
 * @property metadataLinks links of the episode to the external metadata platforms.
 * @see [AdminEpisodeDetailDto]
 */
data class EpisodeWithMetadataLinks(
    val episode: EpisodeWithChildren,
    val metadataLinks: List<EpisodeMetadataLink>
)

/**
 * Maps a stored episode aggregate and its metadata links to the full administration view.
 *
 * @receiver stored episode with its rows and metadata links.
 * @return the full administration view of this episode.
 * @see [AdminEpisodeDetailDto]
 * @see [EpisodeWithMetadataLinks]
 */
fun EpisodeWithMetadataLinks.toAdminDetailDto(): AdminEpisodeDetailDto =
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
        localizations = episode.localizations.map(EpisodeLocalization::toLocalizationAdminDto),
        releases = episode.releases.map(EpisodeRelease::toAdminDto),
        images = episode.images.map(Image::toImageAdminDto),
        metadataLinks = metadataLinks.map(EpisodeMetadataLink::toAdminDto)
    )
