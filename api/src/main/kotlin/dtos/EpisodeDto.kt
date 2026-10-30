package fr.shikkanime.api.dtos

import fr.shikkanime.models.EpisodeLocalization
import fr.shikkanime.models.EpisodePlatform
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.EpisodeWithChildren
import fr.shikkanime.models.Image
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * REST read model of a stored episode, without any persistence metadata.
 *
 * Served by the REST controllers of the episode routes: one DTO shapes the answer of the reads,
 * whatever the caller does with it.
 */
@Serializable
data class EpisodeDto(
    val id: Uuid,
    val animeId: Uuid,
    val season: Int,
    val number: Int,
    val episodeType: EpisodeType,
    val releaseDateTime: LocalDateTime,
    val duration: Long,
    val localizations: List<EpisodeLocalizationDto>,
    val platforms: List<EpisodePlatformDto>,
    val images: List<ImageDto>
)

/**
 * Maps a stored episode aggregate to its API view.
 *
 * Audit columns are deliberately left out of the payload.
 *
 * @receiver stored episode with the rows it owns.
 * @return the API view of this episode, with its localizations, releases and artwork.
 * @see [EpisodeDto]
 * @see [EpisodeWithChildren]
 */
fun EpisodeWithChildren.toDto(): EpisodeDto =
    EpisodeDto(
        id = requireNotNull(episode.id),
        animeId = requireNotNull(episode.animeId),
        season = episode.season,
        number = episode.number,
        episodeType = episode.episodeType,
        releaseDateTime = episode.releaseDateTime,
        duration = episode.duration,
        localizations = localizations.map(EpisodeLocalization::toDto),
        platforms = platforms.map(EpisodePlatform::toDto),
        images = images.map(Image::toDto)
    )
