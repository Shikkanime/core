package fr.shikkanime.api.graphql.queries

import com.expediagroup.graphql.server.operations.Query
import fr.shikkanime.api.dtos.AnimeDto
import fr.shikkanime.api.dtos.toDto
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.models.Anime
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/**
 * GraphQL queries on the stored anime aggregates.
 *
 * The REST controllers stay the canonical read API; this query surface serves the same use cases
 * over a second transport. Both call the very same [AnimeService] and return the very same
 * [AnimeDto]: one DTO, no schema-specific duplicate.
 */
@Suppress("unused")
@Single(binds = [Query::class])
class AnimeQuery(
    private val animeService: AnimeService
) : Query {
    /**
     * Returns every anime persisted in the database.
     *
     * @return every stored anime, in no particular order, as API views.
     * @see AnimeDto
     */
    fun animes(): List<AnimeDto> =
        animeService.findAll().map(Anime::toDto)

    /**
     * Returns one anime persisted in the database.
     *
     * @param id identifier of the anime to load.
     * @return the API view of the anime, or `null` when no anime has this identifier.
     * @see AnimeDto
     */
    fun anime(id: String): AnimeDto? =
        animeService.findById(Uuid.parse(id))?.toDto()
}
