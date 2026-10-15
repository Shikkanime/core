package fr.shikkanime.api.graphql

import com.expediagroup.graphql.server.operations.Query
import fr.shikkanime.api.graphql.AnimeGraphQlDto.Companion.toGraphQl
import fr.shikkanime.database.services.AnimeService
import kotlin.uuid.Uuid

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
     *
     * @return every stored anime, in no particular order, as GraphQL views.
     * @see AnimeGraphQlDto
     */
    fun animes(): List<AnimeGraphQlDto> =
        animeService.findAll().map(::toGraphQl)

    /**
     * Returns one anime persisted in the database.
     *
     * @param id identifier of the anime to load.
     * @return the GraphQL view of the anime, or `null` when no anime has this identifier.
     * @see AnimeGraphQlDto
     */
    fun anime(id: String): AnimeGraphQlDto? =
        animeService.findById(Uuid.parse(id))?.let(::toGraphQl)
}
