package fr.shikkanime.database.repositories

import fr.shikkanime.models.EpisodeObservation
import fr.shikkanime.models.EpisodeRelease
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.Locale
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Persistence port feeding [fr.shikkanime.models.EpisodeGrouper].
 *
 * Reads stay flat: one query returns [EpisodeObservation] rows and one grouped
 * read returns every audio locale of the candidate episodes, so the caller can
 * resolve locale visibility exactly like the grouper does.
 *
 * @see [fr.shikkanime.models.EpisodeObservation]
 * @see [fr.shikkanime.models.EpisodeGrouper]
 */
internal interface EpisodeGroupRepository {
    /**
     * Loads the release observations whose platform release date falls in the window.
     *
     * Every release of every episode is returned — visibility is resolved by the
     * caller, never filtered here, so the grouper sees the same input the legacy
     * indexer indexed.
     *
     * @param from earliest release date included (inclusive).
     * @param to latest release date included (inclusive), or `null` for no upper bound.
     * @param animeId optional anime filter.
     * @param episodeType optional episode type filter.
     * @return the matching observations, unordered.
     */
    fun findObservations(
        from: LocalDateTime,
        to: LocalDateTime?,
        animeId: Uuid?,
        episodeType: EpisodeType?
    ): List<EpisodeObservation>

    /**
     * Loads every audio locale released on each given episode, whatever its date.
     *
     * Locale visibility is an episode-level property: a subtitle track published
     * months ago still makes the English fallback of a recent release invisible,
     * so the read cannot be bounded by the candidate window.
     *
     * @param episodeIds identifiers of the candidate episodes.
     * @return the audio locales of each episode, keyed by episode identifier.
     */
    fun findAudioLocalesByEpisodeIds(episodeIds: List<Uuid>): Map<Uuid, Set<Locale>>

    /**
     * Loads several releases by their identifiers with one query.
     *
     * Group members are flat observations; the platform details of a page are
     * hydrated through this single batch read, never one lookup per release.
     *
     * @param ids identifiers of the releases to load.
     * @return the releases keyed by identifier; an unknown identifier is absent.
     * @see [EpisodeRelease]
     */
    fun findReleasesByIds(ids: Collection<Uuid>): Map<Uuid, EpisodeRelease>
}
