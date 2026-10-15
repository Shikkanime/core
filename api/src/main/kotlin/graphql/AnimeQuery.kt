package fr.shikkanime.api.graphql

import com.expediagroup.graphql.server.operations.Query
import fr.shikkanime.api.dtos.AnimeDto
import fr.shikkanime.api.dtos.ImageDto
import fr.shikkanime.api.dtos.AnimeLocalizationDto
import fr.shikkanime.api.dtos.AnimePlatformDto
import fr.shikkanime.api.dtos.toDto
import fr.shikkanime.database.services.AnimeService

/**
 * GraphQL view of a stored anime, with its identifier as a string so the schema stays scalar-free.
 */
data class AnimeGraphQlDto(
    val id: String,
    val localizations: List<AnimeLocalizationDto>,
    val platforms: List<AnimePlatformDto>,
    val images: List<ImageDto>
)

/**
 * GraphQL queries on the stored anime aggregates.
 *
 * The REST controllers stay the canonical read API; this query surface exists to explore how the
 * pending framework GraphQL module reads the very same [AnimeService] as the REST layer.
 */
@Suppress("unused")
class AnimeQuery(
    private val animeService: AnimeService
) : Query {
    /**
     * Returns every anime persisted in the database.
     */
    fun animes(): List<AnimeGraphQlDto> =
        animeService.findAll().map { it.toDto().toGraphQl() }

    /**
     * Returns one anime persisted in the database, or `null` when it is unknown.
     */
    fun anime(id: String): AnimeGraphQlDto? =
        animeService.findById(kotlin.uuid.Uuid.parse(id))?.toDto()?.toGraphQl()
}

private fun AnimeDto.toGraphQl(): AnimeGraphQlDto =
    AnimeGraphQlDto(
        id = id.toString(),
        localizations = localizations,
        platforms = platforms,
        images = images
    )
