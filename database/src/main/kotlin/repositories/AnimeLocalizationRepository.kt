package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.AnimeLocalizationEntity
import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimeLocalization
import kotlin.uuid.Uuid

/**
 * Persistence port of the [AnimeLocalization] rows of an anime, keyed by locale.
 *
 * @see AnimeRepository
 */
internal abstract class AnimeLocalizationRepository :
    ModelRepository<Uuid, AnimeLocalizationEntity, AnimeLocalization>(AnimeLocalizationEntity) {
    /**
     * Upserts one localization of an anime, keyed by its locale.
     *
     * A known locale is refreshed in place (same row, `updated_at` touched), a new locale is
     * inserted with the supplied identifier. Nothing is deleted.
     *
     * @param anime anime owning the localization, already loaded by the caller.
     * @param localization localized text to persist.
     * @return the localization as stored in database.
     */
    abstract fun save(anime: Anime, localization: AnimeLocalization): AnimeLocalization
}
