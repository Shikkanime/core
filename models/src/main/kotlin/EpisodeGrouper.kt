package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
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
 *    observations whose locale is not visible for [country];
 * 2. filter by [searchTypes] when non-empty;
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

    /** Half-width of the release window around a bucket anchor. */
    private val WINDOW = 2.hours

    /** Time zone used to turn [LocalDateTime] anchors into comparable instants. */
    private val UTC = TimeZone.UTC

    /** Canonical order of observations before bucketing: by release date then release id. */
    private val OBSERVATION_ORDER = compareBy<EpisodeObservation> { it.releaseDateTime }
        .thenBy { it.releaseId }

    /** Order used to pick the surviving sighting of a duplicated `releaseId`. */
    private val SIGHTING_ORDER = compareBy<EpisodeObservation> { it.releaseDateTime }
        .thenBy { it.audioLocale }
        .thenBy { it.episodeId }
        .thenBy { it.animeSlug }

    /** Canonical order of releases inside a group. */
    private val RELEASE_ORDER = compareBy<EpisodeObservation> { it.releaseDateTime }
        .thenBy { it.releaseId }

    /** Canonical order of groups: date, slug, type, season/number range, lang types, release id. */
    private val GROUP_ORDER = compareBy<EpisodeGroup> { it.releaseDateTime }
        .thenBy { it.animeSlug }
        .thenBy { it.episodeType }
        .thenBy { it.minSeason }
        .thenBy { it.minNumber }
        .thenBy { group -> group.coverage.keys.map { it.ordinal }.sorted().joinToString(",") }
        .thenBy { group -> group.releases.minOf { it.releaseId } }

    /** Identity of an episode inside its anime, used for visibility and coverage. */
    private data class EpisodeKey(
        val animeId: Uuid,
        val season: Int,
        val episodeType: EpisodeType,
        val number: Int
    ) {
        fun toCoordinate() = EpisodeCoordinate(season, episodeType, number)
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
     * @param searchTypes when non-empty, keeps only releases whose
     *   [LangType] is listed and drops the groups left empty; `null` or an
     *   empty set means no filter.
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
            .sortedWith(GROUP_ORDER)
    }

    /**
     * Keeps the observations visible for [country], filtered by [searchTypes],
     * deduplicated by `releaseId` and sorted by [OBSERVATION_ORDER].
     */
    private fun visibleObservations(
        observations: List<EpisodeObservation>,
        country: Country,
        searchTypes: Set<LangType>?
    ): List<EpisodeObservation> {
        val visibleByEpisode = observations
            .groupBy { episodeKey(it) }
            .mapValues { (_, episodeObservations) ->
                country.visibleAudioLocales(episodeObservations.map { it.audioLocale }.toSet())
            }

        return observations
            .filter { observation ->
                observation.audioLocale in visibleByEpisode.getValue(episodeKey(observation)) &&
                    (searchTypes.isNullOrEmpty() ||
                        LangType.fromAudioLocale(country, observation.audioLocale) in searchTypes)
            }
            .groupBy { it.releaseId }
            .map { (_, sightings) -> sightings.minWith(SIGHTING_ORDER) }
            .sortedWith(OBSERVATION_ORDER)
    }

    /**
     * Buckets [observations] — already grouped by `(animeId, episodeType)` —
     * into ±2h windows, then splits each bucket by coverage signature.
     */
    private fun splitBucket(
        observations: List<EpisodeObservation>,
        country: Country
    ): List<EpisodeGroup> {
        val langTypeOf = observations.associate { it.releaseId to LangType.fromAudioLocale(country, it.audioLocale) }
        val coverageByLangType = observations
            .groupBy { langTypeOf.getValue(it.releaseId) }
            .mapValues { (_, langObservations) -> langObservations.map { episodeKey(it).toCoordinate() }.toSet() }

        val buckets = mutableListOf<Bucket>()
        for (observation in observations) {
            val instant = toInstant(observation)
            val bucket = buckets.firstOrNull {
                instant <= it.anchor.plus(WINDOW)
            } ?: Bucket(observation.animeId, observation.episodeType, instant)
                .also { buckets += it }
            bucket.observations += observation
        }

        return buckets.flatMap { bucket ->
            bucket.observations
                .groupBy { coverageByLangType.getValue(langTypeOf.getValue(it.releaseId)) }
                .map { (_, members) -> toGroup(members, bucket, coverageByLangType, langTypeOf) }
        }
    }

    /**
     * Builds the [EpisodeGroup] holding [members], a set of observations
     * sharing one coverage signature inside [bucket].
     */
    private fun toGroup(
        members: List<EpisodeObservation>,
        bucket: Bucket,
        coverageByLangType: Map<LangType, Set<EpisodeCoordinate>>,
        langTypeOf: Map<Uuid, LangType>
    ): EpisodeGroup {
        val releases = members.sortedWith(RELEASE_ORDER)
        val coordinates = releases
            .map { episodeKey(it).toCoordinate() }
            .distinct()
            .sortedWith(compareBy({ it.season }, { it.episodeType }, { it.number }))
        val episodes = coordinates.map { coordinate ->
            val member = releases.first { episodeKey(it).toCoordinate() == coordinate }
            EpisodeGroupEpisode(member.episodeId, coordinate.season, coordinate.episodeType, coordinate.number)
        }
        val langTypes = releases.map { langTypeOf.getValue(it.releaseId) }.toSet()
        return EpisodeGroup(
            animeId = bucket.animeId,
            animeSlug = releases.minOf { it.animeSlug },
            episodeType = bucket.episodeType,
            releaseDateTime = releases.first().releaseDateTime,
            minSeason = coordinates.minOf { it.season },
            maxSeason = coordinates.maxOf { it.season },
            minNumber = coordinates.minOf { it.number },
            maxNumber = coordinates.maxOf { it.number },
            coverage = langTypes.associateWith { coverageByLangType.getValue(it) },
            episodes = episodes,
            releases = releases
        )
    }

    /** Episode identity of an observation: `(animeId, season, episodeType, number)`. */
    private fun episodeKey(observation: EpisodeObservation) =
        EpisodeKey(observation.animeId, observation.season, observation.episodeType, observation.number)

    // LocalDateTime carries no arithmetic in this kotlinx-datetime version:
    // the UTC conversion is an exact bijection, so instant comparison is safe.
    private fun toInstant(observation: EpisodeObservation): Instant =
        with(UTC) { observation.releaseDateTime.toInstant() }
}
