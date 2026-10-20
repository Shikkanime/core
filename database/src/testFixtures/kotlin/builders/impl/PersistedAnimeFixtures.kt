package fr.shikkanime.database.builders.impl

import fr.shikkanime.database.entities.AnimeEntity
import fr.shikkanime.models.Anime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

/**
 * Persists one anime row directly through the Exposed DAO for a repository test.
 *
 * The row is inserted outside any repository: the tests of the child repositories need an existing
 * anime to hang their rows on, without mocking the anime repository itself.
 *
 * @return the anime model of the row just inserted.
 * @see [AnimeEntity]
 * @see [AnimeMockKBuilder]
 */
fun savedAnime(): Anime {
    val anime = AnimeMockKBuilder {
        id = Uuid.random()
        normalizedTitle = "One Piece"
    }
        .build()

    transaction { AnimeEntity.new(anime.id) { normalizedTitle = anime.normalizedTitle } }

    return anime
}
