package fr.shikkanime.jobs

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformAnimeLocalization
import fr.shikkanime.jobs.platforms.PlatformImage
import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.Image
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

        return animeService.save(platformAnime.toModel(platform, existing?.id ?: Uuid.random()))
    }
}

private fun PlatformAnime.toModel(platform: Platform, id: Uuid): Anime =
    Anime(
        id = id,
        createdAt = TimeUtils.now(),
        updatedAt = TimeUtils.now(),
        images = images.map { it.toModel(id) },
        localizations = localizations.map(PlatformAnimeLocalization::toModel),
        platforms = listOf(
            AnimePlatform(
                id = Uuid.random(),
                createdAt = TimeUtils.now(),
                updatedAt = TimeUtils.now(),
                platform = platform,
                platformId = this.id
            )
        )
    )

private fun PlatformImage.toModel(animeId: Uuid): Image =
    Image(
        id = Uuid.random(),
        createdAt = TimeUtils.now(),
        updatedAt = TimeUtils.now(),
        entityId = animeId,
        type = type,
        url = url
    )

private fun PlatformAnimeLocalization.toModel(): AnimeLocalization =
    AnimeLocalization(
        id = Uuid.random(),
        createdAt = TimeUtils.now(),
        updatedAt = TimeUtils.now(),
        locale = country.defaultLocale,
        title = title,
        description = description
    )
