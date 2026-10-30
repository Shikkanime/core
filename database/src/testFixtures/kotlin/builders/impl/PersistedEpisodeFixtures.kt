package fr.shikkanime.database.builders.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.entities.AnimeTable
import fr.shikkanime.database.entities.EpisodeEntity
import fr.shikkanime.models.Anime
import fr.shikkanime.models.Episode
import fr.shikkanime.models.EpisodeType
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

/**
 * Persists one episode row directly through the Exposed DAO for a repository test.
 *
 * The row is inserted outside any repository: the tests of the child repositories need an existing
 * episode to hang their rows on, without mocking the episode repository itself.
 *
 * @param anime anime owning the episode, a fresh one when omitted.
 * @return the episode model of the row just inserted.
 * @see [EpisodeEntity]
 * @see [EpisodeMockKBuilder]
 */
fun savedEpisode(anime: Anime = savedAnime()): Episode {
    val episode = EpisodeMockKBuilder {
        id = Uuid.random()
        animeId = anime.id
        season = 1
        number = 1
        episodeType = EpisodeType.EPISODE
        releaseDateTime = TimeUtils.now()
        duration = 1440
    }
        .build()

    transaction {
        EpisodeEntity.new(episode.id) {
            this.anime = EntityID(requireNotNull(anime.id), AnimeTable)
            season = episode.season
            number = episode.number
            episodeType = episode.episodeType
            releaseDateTime = episode.releaseDateTime
            duration = episode.duration
        }
    }

    return episode
}
