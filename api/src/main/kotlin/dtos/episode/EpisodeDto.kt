package fr.shikkanime.api.dtos.episode

import fr.shikkanime.api.dtos.image.ImageDto
import fr.shikkanime.api.dtos.image.toDto
import fr.shikkanime.models.*
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
    val episodeType: EpisodeType,
    val number: Int,
    val releaseDateTime: LocalDateTime,
    val duration: Long,
    val localizations: List<EpisodeLocalizationDto>,
    val releases: List<EpisodeReleaseDto>,
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
        id = requireNotNull(id),
        animeId = requireNotNull(animeId),
        season = season,
        number = number,
        episodeType = episodeType,
        releaseDateTime = releaseDateTime,
        duration = duration,
        localizations = localizations.map(EpisodeLocalization::toDto),
        releases = releases.map(EpisodeRelease::toDto),
        images = images.map(Image::toDto)
    )
