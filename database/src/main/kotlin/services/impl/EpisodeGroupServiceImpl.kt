package fr.shikkanime.database.services.impl

import fr.shikkanime.core.pagination.Page
import fr.shikkanime.core.pagination.PageRequest
import fr.shikkanime.database.repositories.EpisodeGroupRepository
import fr.shikkanime.database.services.EpisodeGroupService
import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeGroupDetail
import fr.shikkanime.models.EpisodeGrouper
import fr.shikkanime.models.EpisodeObservation
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.LangType
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [EpisodeGroupService::class])
internal class EpisodeGroupServiceImpl(
    private val groupRepository: EpisodeGroupRepository
) : EpisodeGroupService {

    private companion object {
        const val DEFAULT_WINDOW_DAYS = 30
        val DEFAULT_WINDOW = DEFAULT_WINDOW_DAYS.days
    }

    private val utcZone = TimeZone.UTC

    override fun findLatest(
        country: Country,
        from: LocalDateTime?,
        to: LocalDateTime?,
        animeId: Uuid?,
        episodeType: EpisodeType?,
        searchTypes: Set<LangType>?,
        request: PageRequest
    ): Page<EpisodeGroupDetail> {
        val observations = groupRepository.findObservations(
            from = from ?: defaultFrom(),
            to = to,
            animeId = animeId,
            episodeType = episodeType
        )

        // Resolve locale visibility per episode over all its releases, then let
        // the grouper recompute it on the filtered rows — the filter is
        // idempotent, so the grouper input matches its own contract.
        val visible = filterVisible(observations, country)

        // The grouper sorts ascending; the endpoint serves the newest groups first
        val groups = EpisodeGrouper.group(visible, country, searchTypes).asReversed()

        val offset = request.offset.toInt()
        val pageGroups = groups.drop(offset).take(request.size)

        // Hydrate the releases of this page only: one batch read, never per group
        val releases = groupRepository.findReleasesByIds(
            pageGroups.flatMap { group -> group.releases.map(EpisodeObservation::releaseId) }
        )

        return Page(
            pageGroups.map { group -> EpisodeGroupDetail(group, releases) },
            request.page,
            request.size,
            groups.size.toLong()
        )
    }

    /**
     * Drops the observations whose audio locale is not visible for [country] on
     * their episode, using every release of that episode — whatever its date.
     *
     * @param observations candidate release rows of the window.
     * @param country market the visibility resolves for.
     * @return the observations visible for the market.
     */
    private fun filterVisible(
        observations: List<EpisodeObservation>,
        country: Country
    ): List<EpisodeObservation> {
        if (observations.isEmpty())
            return emptyList()

        val localesByEpisode = groupRepository.findAudioLocalesByEpisodeIds(
            observations.map(EpisodeObservation::episodeId).distinct()
        )

        return observations.filter { observation ->
            observation.audioLocale in country.visibleAudioLocales(
                localesByEpisode[observation.episodeId].orEmpty()
            )
        }
    }

    /**
     * Computes the default window start: [DEFAULT_WINDOW_DAYS] before now, in UTC.
     *
     * @return the earliest release date included when the caller gives none.
     */
    private fun defaultFrom(): LocalDateTime =
        Clock.System.now().minus(DEFAULT_WINDOW).toLocalDateTime(utcZone)
}
