package fr.shikkanime.api.dtos

import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.Image
import kotlinx.serialization.Serializable

/**
 * API view of a stored anime, without any persistence metadata.
 *
 * Shared by the REST controllers and the GraphQL queries: one DTO serves both transports.
 */
@Serializable
data class AnimeDto(
    val id: String,
    val localizations: List<AnimeLocalizationDto>,
    val platforms: List<AnimePlatformDto>,
    val images: List<ImageDto>
)

/**
 * Maps a stored anime to its API view.
 *
 * Audit columns are deliberately left out of the payload.
 *
 * @return the API view of this anime, with its localizations, sources and artwork.
 * @see AnimeDto
 */
fun Anime.toDto(): AnimeDto =
    AnimeDto(
        id = id.toString(),
        localizations = localizations.map(AnimeLocalization::toDto),
        platforms = platforms.map(AnimePlatform::toDto),
        images = images.map(Image::toDto)
    )
