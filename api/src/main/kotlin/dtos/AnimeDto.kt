package fr.shikkanime.api.dtos

import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Image
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * REST read model of a stored anime, without any persistence metadata.
 *
 * Served by the REST controllers of the anime routes: one DTO shapes the answer of the reads,
 * whatever the caller does with it.
 */
@Serializable
data class AnimeDto(
    val id: Uuid,
    val localizations: List<AnimeLocalizationDto>,
    val platforms: List<AnimePlatformDto>,
    val images: List<ImageDto>
)

/**
 * Maps a stored anime aggregate to its API view.
 *
 * Audit columns are deliberately left out of the payload.
 *
 * @receiver stored anime with the rows it owns.
 * @return the API view of this anime, with its localizations, sources and artwork.
 * @see [AnimeDto]
 * @see [AnimeWithChildren]
 */
fun AnimeWithChildren.toDto(): AnimeDto =
    AnimeDto(
        id = requireNotNull(anime.id),
        localizations = localizations.map(AnimeLocalization::toDto),
        platforms = platforms.map(AnimePlatform::toDto),
        images = images.map(Image::toDto)
    )
