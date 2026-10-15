package fr.shikkanime.api.dtos

import fr.shikkanime.api.dtos.AnimeLocalizationDto.Companion.toDto
import fr.shikkanime.api.dtos.AnimePlatformDto.Companion.toDto
import fr.shikkanime.api.dtos.ImageDto.Companion.toDto
import fr.shikkanime.models.Anime
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
) {
    companion object {
        /**
         * Maps a stored anime to its API view.
         *
         * Audit columns are deliberately left out of the payload.
         *
         * @param anime aggregate read from the database.
         * @return the API view of [anime], with its localizations, sources and artwork.
         * @see Anime
         */
        fun toDto(anime: Anime): AnimeDto =
            AnimeDto(
                id = anime.id,
                localizations = anime.localizations.map(::toDto),
                platforms = anime.platforms.map(::toDto),
                images = anime.images.map(::toDto)
            )
    }
}
