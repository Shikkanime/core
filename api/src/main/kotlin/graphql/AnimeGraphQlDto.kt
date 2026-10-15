package fr.shikkanime.api.graphql

import fr.shikkanime.api.dtos.AnimeLocalizationDto
import fr.shikkanime.api.dtos.AnimeLocalizationDto.Companion.toDto
import fr.shikkanime.api.dtos.AnimePlatformDto
import fr.shikkanime.api.dtos.AnimePlatformDto.Companion.toDto
import fr.shikkanime.api.dtos.ImageDto
import fr.shikkanime.api.dtos.ImageDto.Companion.toDto
import fr.shikkanime.models.Anime

/**
 * GraphQL view of a stored anime, with its identifier as a string so the schema stays scalar-free.
 */
data class AnimeGraphQlDto(
    val id: String,
    val localizations: List<AnimeLocalizationDto>,
    val platforms: List<AnimePlatformDto>,
    val images: List<ImageDto>
) {
    companion object {
        /**
         * Maps a stored anime to its GraphQL view.
         *
         * The identifier is rendered as a string because the GraphQL schema carries no Uuid scalar.
         *
         * @param anime aggregate read from the database.
         * @return the GraphQL view of [anime], with its localizations, sources and artwork.
         * @see Anime
         */
        fun toGraphQl(anime: Anime): AnimeGraphQlDto =
            AnimeGraphQlDto(
                id = anime.id.toString(),
                localizations = anime.localizations.map(::toDto),
                platforms = anime.platforms.map(::toDto),
                images = anime.images.map(::toDto)
            )
    }
}
