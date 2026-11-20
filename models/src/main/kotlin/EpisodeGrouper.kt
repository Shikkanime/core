package fr.shikkanime.models

import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Pure, stateless port of the legacy `NewGroupIndexer`: groups flat
 * [EpisodeObservation] rows into [EpisodeGroup]s for one [Country].
 *
 * Algorithm (see `EpisodeGrouperTest` for the executable spec):
 * 1. resolve the visible audio locales of every episode and drop the
 *    observations whose locale is not visible for the market;
 * 2. filter by `searchTypes` when non-empty;
 * 3. deduplicate by `releaseId`, keeping the earliest `releaseDateTime`;
 * 4. bucket by `(animeId, episodeType)` with an inclusive ±2h window anchored
 *    on the bucket's first observation (sorted input, non-transitive);
 * 5. inside a bucket, split by coverage signature: the set of episode
 *    coordinates covered by each [LangType] across the whole bucket —
 *    observations merge when their lang types cover exactly the same
 *    coordinates;
 * 6. sort groups by `(releaseDateTime, animeSlug, episodeType, minSeason,
 *    minNumber)`, then by lang-type set ordinal, then by smallest release id.
 */
object EpisodeGrouper {

    // Half-width of the release window around a bucket anchor; on sorted
    // input only the upper bound is tested, the lower one is implicit.
    private val window = 2.hours

    // Time zone used to turn LocalDateTime anchors into comparable instants.
    private val utcZone = TimeZone.UTC

    // Canonical order of observations before bucketing: by release date then release id.
    private val observationOrder = compareBy(
        EpisodeObservation::releaseDateTime,
        EpisodeObservation::releaseId
    )

    // Order used to pick the surviving sighting of a duplicated releaseId — total order.
    private val sightingOrder = compareBy(
        EpisodeObservation::releaseDateTime,
        EpisodeObservation::audioLocale,
        EpisodeObservation::animeId,
        EpisodeObservation::episodeId,
        EpisodeObservation::season,
        EpisodeObservation::episodeType,
        EpisodeObservation::number,
        EpisodeObservation::animeSlug
    )

    // Canonical order of releases inside a group.
    private val releaseOrder = compareBy(
        EpisodeObservation::releaseDateTime,
        EpisodeObservation::releaseId
    )

    // Canonical order of groups: date, slug, type, season/number range, lang types, release id.
    private val groupOrder = compareBy(
        EpisodeGroup::releaseDateTime,
        EpisodeGroup::animeSlug,
        EpisodeGroup::episodeType,
        EpisodeGroup::minSeason,
        EpisodeGroup::minNumber
    )
        .thenComparator { a, b -> compareLangTypeSets(a.coverage.keys, b.coverage.keys) }
        .thenBy { group -> group.releases.minOf(EpisodeObservation::releaseId) }

    /** Identity of an episode inside its anime, used for visibility and coverage. */
    private data class EpisodeKey(
        val animeId: Uuid,
        val season: Int,
        val episodeType: EpisodeType,
        val number: Int
    ) {
        fun toCoordinate(): EpisodeCoordinate = EpisodeCoordinate(season, episodeType, number)
    }

    /** Bucket key: the `(animeId, episodeType)` pair observations are grouped under. */
    private data class BucketKey(val animeId: Uuid, val episodeType: EpisodeType)

    /** A ±2h bucket of observations sharing one [BucketKey], anchored on its earliest release. */
    private data class Bucket(
        val animeId: Uuid,
        val episodeType: EpisodeType,
        val anchor: Instant,
        val observations: MutableList<EpisodeObservation> = mutableListOf()
    )

    /**
     * Groups [observations] for [country]. The input order does not matter:
     * the output is fully deterministic.
     *
     * @param observations flat release rows, one per episode release.
     * @param country the market the locales are resolved for.
     * @param searchTypes when non-empty, keeps only releases whose
     *   [LangType] is listed and drops the groups left empty; `null` or an
     *   empty set means no filter.
     * @return the groups sorted by [groupOrder].
     * @see EpisodeGroup
     * @see Country.visibleAudioLocales
     */
    fun group(
        observations: List<EpisodeObservation>,
        country: Country,
        searchTypes: Set<LangType>? = null
    ): List<EpisodeGroup> {
        val visible = visibleObservations(observations, country, searchTypes)
        if (visible.isEmpty()) return emptyList()

        return visible
            .groupBy { BucketKey(it.animeId, it.episodeType) }
            .flatMap { (_, bucketObservations) -> splitBucket(bucketObservations, country) }
            .sortedWith(groupOrder)
    }

    /**
     * Keeps the observations visible for [country], filtered by [searchTypes],
     * deduplicated by `releaseId` and sorted by [observationOrder].
     *
     * @param observations the raw release rows.
     * @param country the market the locales are resolved for.
     * @param searchTypes optional [LangType] filter; `null`/empty means none.
     * @return the sorted, deduplicated, visible observations.
     */
    private fun visibleObservations(
        observations: List<EpisodeObservation>,
        country: Country,
        searchTypes: Set<LangType>?
    ): List<EpisodeObservation> {
        val visibleByEpisode = observations
            .groupBy { episodeKey(it) }
            .mapValues { (_, episodeObservations) ->
                country.visibleAudioLocales(episodeObservations.map(EpisodeObservation::audioLocale).toSet())
            }

        return observations
            .filter { observation ->
                observation.audioLocale in visibleByEpisode.getValue(episodeKey(observation)) &&
                    (searchTypes.isNullOrEmpty() ||
                        LangType.fromAudioLocale(country, observation.audioLocale) in searchTypes)
            }
            .groupBy(EpisodeObservation::releaseId)
            .map { (_, sightings) -> sightings.minWith(sightingOrder) }
            .sortedWith(observationOrder)
    }

    /**
     * Buckets [observations] — already grouped by `(animeId, episodeType)` and
     * sorted by [observationOrder] — into ±2h windows, then splits each bucket
     * by coverage signature.
     *
     * @param observations visible observations of a single `(animeId, episodeType)`.
     * @param country the market the [LangType]s are resolved for.
     * @return the groups of this bucket key.
     */
    private fun splitBucket(
        observations: List<EpisodeObservation>,
        country: Country
    ): List<EpisodeGroup> {
        val langTypeOf = observations.associate { it.releaseId to LangType.fromAudioLocale(country, it.audioLocale) }

        // Anchors are ascending and expired buckets form a prefix of the deque,
        // so the front is always the first bucket the observation can join.
        // Expired buckets are archived, never dropped: they still emit groups.
        val expired = mutableListOf<Bucket>()
        val active = observations.fold(ArrayDeque<Bucket>()) { deque, observation ->
            val instant = toInstant(observation)
            while (deque.firstOrNull()?.let { instant > it.anchor.plus(window) } == true) {
                expired += deque.removeFirst()
            }
            val bucket = deque.firstOrNull()
                ?: Bucket(observation.animeId, observation.episodeType, instant)
                    .also { deque.addLast(it) }
            bucket.observations += observation
            deque
        }
        val buckets = expired + active

        return buckets.flatMap { bucket ->
            // The coverage signature is computed per bucket, like the legacy
            // NewGroupIndexer: episodes outside this ±2h window must not leak
            // into the signature.
            val coverageByLangType = bucket.observations
                .groupBy { langTypeOf.getValue(it.releaseId) }
                .mapValues { (_, langObservations) ->
                    langObservations.map { episodeKey(it).toCoordinate() }.toSet()
                }
            bucket.observations
                .groupBy { coverageByLangType.getValue(langTypeOf.getValue(it.releaseId)) }
                .map { (_, members) -> toGroup(members, bucket, coverageByLangType, langTypeOf) }
        }
    }

    /**
     * Builds the [EpisodeGroup] holding [members], a set of observations
     * sharing one coverage signature inside [bucket].
     *
     * @param members observations sharing the same coverage signature.
     * @param bucket the ±2h bucket [members] belong to.
     * @param coverageByLangType per-lang-type covered coordinates of the bucket.
     * @param langTypeOf [LangType] of each release, indexed by `releaseId`.
     * @return the populated group.
     */
    private fun toGroup(
        members: List<EpisodeObservation>,
        bucket: Bucket,
        coverageByLangType: Map<LangType, Set<EpisodeCoordinate>>,
        langTypeOf: Map<Uuid, LangType>
    ): EpisodeGroup {
        val releases = members.sortedWith(releaseOrder)
        val coordinates = releases
            .map { episodeKey(it).toCoordinate() }
            .distinct()
            .sortedWith(compareBy(EpisodeCoordinate::season, EpisodeCoordinate::episodeType, EpisodeCoordinate::number))
        val firstByCoordinate = releases.groupBy { episodeKey(it).toCoordinate() }
            .mapValues { (_, coordinateReleases) -> coordinateReleases.first() }
        val episodes = coordinates.map { coordinate ->
            val member = firstByCoordinate.getValue(coordinate)
            EpisodeGroupEpisode(member.episodeId, coordinate.season, coordinate.episodeType, coordinate.number)
        }
        val langTypes = releases.map { langTypeOf.getValue(it.releaseId) }.toSet()
        return EpisodeGroup(
            animeId = bucket.animeId,
            animeSlug = releases.minOf(EpisodeObservation::animeSlug),
            episodeType = bucket.episodeType,
            releaseDateTime = releases.first().releaseDateTime,
            minSeason = coordinates.minOf(EpisodeCoordinate::season),
            maxSeason = coordinates.maxOf(EpisodeCoordinate::season),
            minNumber = coordinates.minOf(EpisodeCoordinate::number),
            maxNumber = coordinates.maxOf(EpisodeCoordinate::number),
            coverage = langTypes.associateWith { coverageByLangType.getValue(it) },
            episodes = episodes,
            releases = releases
        )
    }

    /** Compares two lang-type sets by their sorted ordinals, element by element. */
    private fun compareLangTypeSets(a: Set<LangType>, b: Set<LangType>): Int {
        val ordinalsA = a.map(LangType::ordinal).sorted()
        val ordinalsB = b.map(LangType::ordinal).sorted()
        return (0 until maxOf(ordinalsA.size, ordinalsB.size))
            .map { index -> (ordinalsA.getOrNull(index) ?: -1).compareTo(ordinalsB.getOrNull(index) ?: -1) }
            .firstOrNull { it != 0 }
            ?: 0
    }

    /** Episode identity of an observation: `(animeId, season, episodeType, number)`. */
    private fun episodeKey(observation: EpisodeObservation): EpisodeKey =
        EpisodeKey(observation.animeId, observation.season, observation.episodeType, observation.number)

    // LocalDateTime carries no arithmetic in this kotlinx-datetime version:
    // the UTC conversion is an exact bijection, so instant comparison is safe.
    private fun toInstant(observation: EpisodeObservation): Instant =
        with(utcZone) { observation.releaseDateTime.toInstant() }
}
