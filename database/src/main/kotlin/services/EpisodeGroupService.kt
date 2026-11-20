package fr.shikkanime.database.services

import fr.shikkanime.core.pagination.Page
import fr.shikkanime.core.pagination.PageRequest
import fr.shikkanime.exposed.Transactional
import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeGroupDetail
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.LangType
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Read service listing episode groups the way the legacy grouped indexer did.
 *
 * Groups are computed on every call from the stored releases — there is no
 * derived index to rebuild, so a restarted API serves the same data the jobs
 * persisted. Locale visibility is resolved per episode over all of its
 * releases before grouping, exactly like [fr.shikkanime.models.EpisodeGrouper]
 * expects.
 *
 * @see [fr.shikkanime.models.EpisodeGrouper]
 * @see [fr.shikkanime.models.EpisodeGroup]
 */
interface EpisodeGroupService {
    /**
     * Lists one page of episode groups, most recently released first.
     *
     * When [from] is `null` the window defaults to the last [DEFAULT_WINDOW_DAYS]
     * days; [to] left `null` keeps future-dated releases. Groups are computed on
     * the whole windowed set, then paginated — a page never splits a group.
     *
     * @param country market the locale visibility and lang types resolve for.
     * @param from earliest release date included, or `null` for the default window.
     * @param to latest release date included, or `null` for no upper bound.
     * @param animeId optional anime filter.
     * @param episodeType optional episode type filter.
     * @param searchTypes optional lang-type filter applied before grouping.
     * @param request page and size to read.
     * @return the requested page of hydrated groups, newest first.
     * @see [fr.shikkanime.models.EpisodeGroupDetail]
     */
    @Transactional
    fun findLatest(
        country: Country,
        from: LocalDateTime?,
        to: LocalDateTime?,
        animeId: Uuid?,
        episodeType: EpisodeType?,
        searchTypes: Set<LangType>?,
        request: PageRequest
    ): Page<EpisodeGroupDetail>
}
