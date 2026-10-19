package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.AnimeLocalizationEntity
import fr.shikkanime.models.AnimeLocalization
import kotlin.uuid.Uuid

/**
 * Persistence port of the [AnimeLocalization] rows of an anime, keyed by locale.
 *
 * @see [AnimeRepository]
 */
internal abstract class AnimeLocalizationRepository :
    ModelRepository<Uuid, AnimeLocalizationEntity, AnimeLocalization>(AnimeLocalizationEntity) {
    /**
     * Loads every localization of several animes with one query, grouped by owner.
     *
     * The aggregate composition must not run one lookup per anime: a whole page resolves its texts
     * through this single call.
     *
     * @param animeIds identifiers of the animes owning the localizations.
     * @return the localizations of each anime, an empty list for an anime without text.
     * @see [AnimeLocalization]
     */
    abstract fun findAllByAnimeIds(animeIds: List<Uuid>): Map<Uuid?, List<AnimeLocalization>>

    /**
     * Upserts one localization of an anime, keyed by its locale.
     *
     * A known locale is refreshed in place (same row, `updated_at` touched), a new locale is
     * inserted with the supplied identifier. Nothing is deleted.
     *
     * @param localization localized text to persist; its `animeId` must reference the anime.
     * @return the localization as stored in database.
     * @see [AnimeLocalization]
     */
    abstract fun save(localization: AnimeLocalization): AnimeLocalization
}
