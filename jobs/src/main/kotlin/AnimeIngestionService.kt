package fr.shikkanime.jobs

import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.models.Anime
import fr.shikkanime.models.Platform
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/**
 * Saves the animes harvested from a streaming platform into the database.
 *
 * An anime is identified across runs by its platform source link: when the link already points to
 * a stored anime, that anime is updated in place through [AnimeService.save], so a second run of
 * the same harvest never duplicates a row.
 */
@Single
internal class AnimeIngestionService(
    private val animeService: AnimeService
) {
    /**
     * Upserts every anime harvested from a platform.
     *
     * @param platform streaming platform the animes were harvested from.
     * @param animes normalized animes to persist, without duplicates.
     * @return the number of animes passed to the persistence.
     */
    fun saveAll(platform: Platform, animes: List<PlatformAnime>): Int {
        animes.forEach { save(platform, it) }

        return animes.size
    }

    /**
     * Upserts one harvested anime, reusing the stored anime behind its platform link.
     *
     * @param platform streaming platform the anime was harvested from.
     * @param platformAnime normalized anime to persist.
     * @return the anime as stored in database.
     */
    private fun save(platform: Platform, platformAnime: PlatformAnime): Anime {
        val existing = animeService.findByPlatformAndPlatformId(platform, platformAnime.id)

        // The fresh model carries no identity; the stored anime behind the platform link owns it.
        return animeService.save(
            platformAnime.toModel()
                .copy(id = existing?.id)
        )
    }
}
