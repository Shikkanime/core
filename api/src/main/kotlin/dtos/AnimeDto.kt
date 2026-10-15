package fr.shikkanime.api.dtos

import fr.shikkanime.models.Anime
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * API view of a stored anime, without any persistence metadata.
 */
@Serializable
data class AnimeDto(
    val id: Uuid,
    val localizations: List<AnimeLocalizationDto>,
    val platforms: List<AnimePlatformDto>,
    val images: List<ImageDto>
)

/**
 * API view of a localized editorial text.
 */
@Serializable
data class AnimeLocalizationDto(
    val locale: Locale,
    val title: String,
    val description: String?
)

/**
 * API view of a platform source link.
 */
@Serializable
data class AnimePlatformDto(
    val platform: Platform,
    val platformId: String
)

/**
 * API view of an artwork row.
 */
@Serializable
data class ImageDto(
    val type: ImageType,
    val url: String
)

/**
 * Maps a stored anime to its API view.
 *
 * Audit columns are deliberately left out of the payload.
 */
fun Anime.toDto(): AnimeDto =
    AnimeDto(
        id = id,
        localizations = localizations.map {
            AnimeLocalizationDto(
                locale = it.locale,
                title = it.title,
                description = it.description
            )
        },
        platforms = platforms.map {
            AnimePlatformDto(
                platform = it.platform,
                platformId = it.platformId
            )
        },
        images = images.map {
            ImageDto(
                type = it.type,
                url = it.url
            )
        }
    )
