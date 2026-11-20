package fr.shikkanime.jobs.metadata

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.database.services.AnimeMetadataLinkService
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.database.services.EpisodeMetadataLinkService
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.jobs.metadata.MetadataLinker.Companion.SEASON_PATTERNS
import fr.shikkanime.models.*
import org.koin.core.annotation.Single

/**
 * Links every stored anime and episode to its entries on the external metadata platforms.
 *
 * The matching is title-driven: each stored anime is searched on AniList, MyAnimeList and
 * AniDB, the candidates are scored against the stored titles, and the best entry per platform
 * - plus the season entries of multi-season franchises - is persisted as a metadata link.
 * Episodes are then paired with the episode entries of the linked external animes, by number
 * when the platform numbers them and by rank inside their season entry otherwise.
 *
 * AniList exposes no per-episode data, so episodes are only linked on MyAnimeList and AniDB.
 */
@Single
class MetadataLinker(
    private val animeService: AnimeService,
    private val episodeService: EpisodeService,
    private val animeMetadataLinkService: AnimeMetadataLinkService,
    private val episodeMetadataLinkService: EpisodeMetadataLinkService,
    private val anilistClient: AniListClient,
    private val myAnimeListClient: MyAnimeListClient,
    private val aniDbClient: AniDbClient
) {
    /**
     * Summary of one linking pass.
     *
     * @param animesProcessed number of stored animes examined.
     * @param animeLinksSaved number of anime metadata links persisted.
     * @param episodeLinksSaved number of episode metadata links persisted.
     * @param unmatched animes for which no entry could be found on a platform, with the missing
     * platforms.
     * @param assignments the external identifiers chosen per anime and platform, for audit.
     */
    data class LinkReport(
        val animesProcessed: Int,
        val animeLinksSaved: Int,
        val episodeLinksSaved: Int,
        val unmatched: Map<String, Set<MetadataPlatform>>,
        val assignments: Map<String, Map<MetadataPlatform, Set<String>>> = emptyMap()
    )

    /**
     * Outcome of the linking of one stored anime.
     *
     * @param animeLinks number of anime metadata links persisted.
     * @param episodeLinks number of episode metadata links persisted.
     * @param missing platforms on which no entry could be matched.
     * @param assignments the external identifiers chosen per platform, for audit.
     */
    private data class AnimeLinkResult(
        val animeLinks: Int,
        val episodeLinks: Int,
        val missing: Set<MetadataPlatform>,
        val assignments: Map<MetadataPlatform, Set<String>> = emptyMap()
    )

    /**
     * One external entry and the stored seasons it answers for.
     */
    private data class SeasonAssignment(
        val externalId: String,
        val seasons: Set<Int>,
        val episodeOffset: Int = 0
    )

    private val logger = LoggerFactory.getLogger()

    /**
     * Links every stored anime and its episodes to the metadata platforms.
     *
     * @param persist when false, the matching runs and logs but nothing is written: the dry run
     * used to audit the matches before persisting them.
     * @return the summary of the pass.
     */
    suspend fun linkAll(persist: Boolean = true): LinkReport {
        val animes = animeService.findAll()
        val results = animes.map { anime ->
            anime.normalizedTitle to linkAnime(anime, persist)
        }

        return LinkReport(
            animes.size,
            results.sumOf { it.second.animeLinks },
            results.sumOf { it.second.episodeLinks },
            results.mapNotNull { (title, result) ->
                result.missing.takeIf { it.isNotEmpty() }?.let { title to it }
            }.toMap(),
            results.mapNotNull { (title, result) ->
                result.assignments.takeIf { it.isNotEmpty() }?.let { title to it }
            }.toMap()
        )
    }

    /**
     * Links one stored anime and every episode it owns to the metadata platforms.
     *
     * The stored titles drive the candidate search on AniList, MyAnimeList and AniDB; the best
     * entry per platform - plus the season entries of multi-season franchises - is persisted as
     * a metadata link, then the episodes covered by each assignment are paired with the episode
     * entries of the external anime.
     *
     * @param anime stored anime aggregate to link.
     * @param persist when false, the matching runs and logs but nothing is written.
     * @return the counts of links persisted and the platforms left unmatched.
     * @see [linkAll]
     * @see [resolveAssignments]
     */
    private suspend fun linkAnime(
        anime: AnimeWithChildren,
        persist: Boolean
    ): AnimeLinkResult {
        val normalizedTitle = anime.normalizedTitle
        val animeId = requireNotNull(anime.id)
        val episodes = episodeService.findAllByAnimeId(animeId).map { it.episode }
        val ourTitles = (anime.localizations.map(AnimeLocalization::title) + normalizedTitle).distinct()
        val seasons = episodes.filter { it.episodeType == EpisodeType.EPISODE }
            .map { it.season }
            .toSet()
            .ifEmpty { setOf(1) }

        val assignments = resolveAssignments(normalizedTitle, ourTitles, seasons)
        var animeLinks = 0
        var episodeLinks = 0

        assignments.forEach { (platform, platformAssignments) ->
            platformAssignments.forEach { assignment ->
                if (persist) {
                    animeMetadataLinkService.save(
                        AnimeMetadataLink(
                            id = null, createdAt = null, updatedAt = null,
                            animeId = animeId,
                            platform = platform,
                            platformId = assignment.externalId
                        )
                    )
                }
                animeLinks++
                episodeLinks += linkEpisodes(episodes, platform, assignment, persist)
            }
        }

        val missing = MetadataPlatform.entries.toSet() - assignments.keys
        if (missing.isNotEmpty())
            logger.warning("No metadata match for '$normalizedTitle' on $missing")

        val detail = assignments.mapValues { (_, list) ->
            list.map { it.externalId }.toSet()
        }
        return AnimeLinkResult(animeLinks, episodeLinks, missing, detail)
    }

    /**
     * Resolves, per platform, which external entries the stored anime points at.
     *
     * Each assignment maps one external entry to the stored seasons it covers: a franchise
     * split into one entry per season gets one assignment per season, a continuous catalog
     * entry (AniDB long runners) gets every season.
     *
     * @param normalizedTitle normalized title of the stored anime, also the key of the manual
     * overrides.
     * @param ourTitles titles the stored anime is known under.
     * @param seasons stored seasons the external entries must cover.
     * @return the assignments per platform, empty for platforms left unmatched.
     * @see [assignSeasons]
     * @see [MetadataOverrides]
     */
    private suspend fun resolveAssignments(
        normalizedTitle: String,
        ourTitles: List<String>,
        seasons: Set<Int>
    ): Map<MetadataPlatform, List<SeasonAssignment>> {
        val override = MetadataOverrides.anime[normalizedTitle]

        val anilistCandidates = if (MetadataPlatform.ANILIST in override.orEmpty().keys) {
            emptyList()
        } else {
            gatherAniListCandidates(ourTitles)
        }
        val anidbCandidates = if (MetadataPlatform.ANIDB in override.orEmpty().keys) {
            emptyList()
        } else {
            gatherAniDbCandidates(ourTitles)
        }

        val anilist = override?.get(MetadataPlatform.ANILIST)
            ?.map { SeasonAssignment(it.id, it.seasons, it.episodeOffset) }
            ?: assignSeasons(
                MetadataPlatform.ANILIST, anilistCandidates, ourTitles, seasons
            )

        val anidb = override?.get(MetadataPlatform.ANIDB)
            ?.map { SeasonAssignment(it.id, it.seasons, it.episodeOffset) }
            ?: assignSeasons(
                MetadataPlatform.ANIDB, anidbCandidates, ourTitles, seasons
            )

        // MyAnimeList candidates come from its own title search plus the identifiers the
        // other platforms declare; manual overrides bypass the scoring.
        val malOverride = override?.get(MetadataPlatform.MYANIMELIST)
        val malAssignments = if (malOverride != null) {
            malOverride.map { SeasonAssignment(it.id, it.seasons, it.episodeOffset) }
        } else {
            val malCandidates = LinkedHashMap<String, ExternalAnime>()
            ourTitles.flatMap { myAnimeListClient.searchAnime(it) }
                .forEach { malCandidates.putIfAbsent(it.id, it) }
            (anilistCandidates + anidbCandidates)
                .mapNotNull { it.externalIds[MetadataPlatform.MYANIMELIST] }
                .forEach { id ->
                    if (id !in malCandidates.keys) {
                        id.toIntOrNull()
                            ?.let { myAnimeListClient.getAnimeById(it) }
                            ?.let { malCandidates.putIfAbsent(it.id, it) }
                    }
                }

            // Search stubs only carry the titles the index exposes; the full entries add the
            // localized titles and counts the matching relies on. Only the best few stubs are
            // resolved, the rest stay cheap.
            malCandidates.values
                .map { it to scoreCandidate(it, ourTitles) }
                .sortedByDescending { it.second }
                .take(MAX_MAL_ENRICHMENTS)
                .forEach { (stub, _) ->
                    stub.id.toIntOrNull()
                        ?.let { myAnimeListClient.getAnimeById(it) }
                        ?.let { malCandidates[it.id] = it }
                }

            // Franchise siblings share titles ("Sgt. Frog" names both Keroro Gunsou and
            // KERORO): the MAL identifier declared by the already-matched platforms is the
            // tiebreaker.
            val selectedIds = (anilist + anidb).map { it.externalId }.toSet()
            val preferredMalIds = (anilistCandidates + anidbCandidates)
                .filter { it.id in selectedIds }
                .mapNotNull { it.externalIds[MetadataPlatform.MYANIMELIST] }
                .toSet()

            assignSeasons(
                MetadataPlatform.MYANIMELIST,
                malCandidates.values.toList(),
                ourTitles,
                seasons,
                preferredIds = preferredMalIds
            )
        }

        logger.info(
            "Candidates for '$normalizedTitle': " +
                "anilist=${anilistCandidates.size} (${anilist.size} assignment(s)), " +
                "anidb=${anidbCandidates.size} (${anidb.size}), " +
                "mal=${malAssignments.size} assignment(s)"
        )

        return buildMap {
            if (anilist.isNotEmpty()) put(MetadataPlatform.ANILIST, anilist)
            if (malAssignments.isNotEmpty()) put(MetadataPlatform.MYANIMELIST, malAssignments)
            if (anidb.isNotEmpty()) put(MetadataPlatform.ANIDB, anidb)
        }
    }

    /**
     * Collects the AniList candidates of the stored titles.
     *
     * Every title is searched, duplicates are merged by identifier, then the best candidates are
     * expanded through their relations so the season entries of a franchise become candidates too.
     *
     * @param ourTitles titles the stored anime is known under.
     * @return the deduplicated AniList candidates.
     * @see [AniListClient.searchAnime]
     * @see [scoreCandidate]
     */
    private suspend fun gatherAniListCandidates(ourTitles: List<String>): List<ExternalAnime> {
        val candidates = LinkedHashMap<String, ExternalAnime>()
        ourTitles.flatMap { anilistClient.searchAnime(it) }
            .forEach { candidates.putIfAbsent(it.id, it) }

        // Season entries of a franchise hang from the relations of the main entry: the best
        // candidates are expanded once so the season variants become candidates too.
        val expandable = candidates.values
            .map { it to scoreCandidate(it, ourTitles) }
            .filter { it.second >= EXPAND_SCORE_THRESHOLD }
            .sortedByDescending { it.second }
            .take(MAX_EXPANSIONS)
        val relatedIds = expandable
            .flatMap { it.first.relatedIds }
            .mapNotNull { it.toIntOrNull() }
            .toSet()
            .minus(candidates.keys.mapNotNull { it.toIntOrNull() }.toSet())
        if (relatedIds.isNotEmpty()) {
            anilistClient.getAnimeByIds(relatedIds)
                .forEach { candidates.putIfAbsent(it.id, it) }
        }

        return candidates.values.toList()
    }

    /**
     * Collects the AniDB candidates of the stored titles.
     *
     * Titles are searched first; the best stubs are then resolved into full entries, and the
     * stubs are kept as fallback when the bulk fetch answers nothing.
     *
     * @param ourTitles titles the stored anime is known under.
     * @return the AniDB candidates, full entries when available.
     * @see [AniDbClient.searchAnime]
     */
    private suspend fun gatherAniDbCandidates(ourTitles: List<String>): List<ExternalAnime> {
        val stubs = LinkedHashMap<String, ExternalAnime>()
        ourTitles.flatMap { aniDbClient.searchAnime(it) }
            .forEach { stubs.putIfAbsent(it.id, it) }
        val ids = stubs.values.take(MAX_ANI_DB_CANDIDATES)
            .mapNotNull { it.id.toIntOrNull() }
        val full = aniDbClient.getAnimeByIds(ids)
        return full.ifEmpty { stubs.values.toList() }
    }

    /**
     * Picks the external entries covering [seasons] among [candidates].
     *
     * A candidate whose titles carry a season marker is assigned to that season; a candidate
     * without marker answers the lowest stored season, and on AniDB - where long runners keep
     * a single continuous entry - it answers every season.
     *
     * @param platform metadata platform the candidates belong to.
     * @param candidates external entries scored against the stored titles.
     * @param ourTitles titles the stored anime is known under.
     * @param seasons stored seasons the external entries must cover.
     * @param preferredIds external identifiers declared by the other platforms, used to break
     * ties between comparable title scores.
     * @return the external entries assigned to the stored seasons.
     * @see [scoreCandidate]
     */
    private fun assignSeasons(
        platform: MetadataPlatform,
        candidates: List<ExternalAnime>,
        ourTitles: List<String>,
        seasons: Set<Int>,
        preferredIds: Set<String> = emptySet()
    ): List<SeasonAssignment> {
        if (candidates.isEmpty()) return emptyList()

        val scored = candidates
            .map { it to scoreCandidate(it, ourTitles) }
        val best = scored.maxByOrNull { it.second }!!

        if (best.second < MATCH_SCORE_THRESHOLD) {
            logger.info(
                "No confident $platform match for '${ourTitles.firstOrNull()}': " +
                    "best=${best.first.id} score=${best.second}"
            )
            return emptyList()
        }

        // Cross-platform identifiers only break ties between comparable title scores: they
        // order the candidates but never lower the title bar for a match.
        val ranked = scored.sortedByDescending { (candidate, score) ->
            score + if (candidate.id in preferredIds)
                PREFERRED_ID_BONUS
            else
                0.0
        }

        fun seasonMarkerOf(candidate: ExternalAnime): Int? =
            candidate.titles.mapNotNull(::extractSeasonMarker).maxOrNull()

        val assignments = mutableListOf<SeasonAssignment>()
        val assignedIds = mutableSetOf<String>()

        seasons.sorted().forEach { season ->
            val marked = ranked.firstOrNull { (candidate, _) ->
                seasonMarkerOf(candidate) == season
            }
            if (marked != null) {
                assignments += SeasonAssignment(marked.first.id, setOf(season))
                assignedIds += marked.first.id
                return@forEach
            }

            val unmarked = ranked.firstOrNull { (candidate, _) ->
                seasonMarkerOf(candidate) == null && candidate.id !in assignedIds
            }
            if (unmarked != null &&
                (platform == MetadataPlatform.ANIDB || season == seasons.min())) {
                val covered = if (platform == MetadataPlatform.ANIDB)
                    seasons
                else
                    setOf(season)
                assignments += SeasonAssignment(unmarked.first.id, covered)
                assignedIds += unmarked.first.id
            }
        }

        return assignments.distinctBy { it.externalId }
    }

    /**
     * Scores one external candidate against the stored titles.
     *
     * The score is the best title similarity over every stored title, capped at `1.0`.
     *
     * @param candidate external entry to score.
     * @param ourTitles titles the stored anime is known under.
     * @return the matching score between `0.0` and `1.0`.
     * @see [TitleMatcher.bestScore]
     */
    private fun scoreCandidate(candidate: ExternalAnime, ourTitles: List<String>): Double {
        val titleScore = ourTitles.maxOfOrNull { TitleMatcher.bestScore(candidate.titles, it) }
            ?: 0.0
        return titleScore.coerceAtMost(1.0)
    }

    /**
     * Extracts the season number a title announces, when it announces one.
     *
     * Recognizes "Season 2", "2nd Season", "2期"/"第2期" and "S2" markers.
     *
     * @param title external title to inspect.
     * @return the announced season number, or `null` when the title carries no marker.
     * @see [SEASON_PATTERNS]
     */
    private fun extractSeasonMarker(title: String): Int? =
        SEASON_PATTERNS.firstNotNullOfOrNull { pattern ->
            pattern.find(title)?.groupValues?.get(1)?.toIntOrNull()
        }

    /**
     * Pairs the stored episodes covered by one assignment with the episode entries of the
     * external anime and persists the links.
     *
     * Episodes are matched by number first - shifted by the assignment offset when the platform
     * numbers per entry - and by rank inside their season entry as fallback, but only for a
     * dense one-to-one pairing. AniList is skipped: it exposes no per-episode data.
     *
     * @param episodes stored episodes of the anime.
     * @param platform metadata platform the assignment belongs to.
     * @param assignment external entry and the stored seasons it covers.
     * @param persist when false, the matching runs and logs but nothing is written.
     * @return the number of episode links persisted.
     * @see [SeasonAssignment]
     */
    private suspend fun linkEpisodes(
        episodes: List<Episode>,
        platform: MetadataPlatform,
        assignment: SeasonAssignment,
        persist: Boolean
    ): Int {
        if (platform == MetadataPlatform.ANILIST) return 0

        val ourEpisodes = episodes
            .filter { it.episodeType == EpisodeType.EPISODE && it.season in assignment.seasons }
            .sortedBy { it.number }
        // A franchise reference owns no stored season: skip the external fetch entirely.
        if (ourEpisodes.isEmpty()) return 0

        val id = assignment.externalId.toIntOrNull() ?: return 0
        val external = when (platform) {
            MetadataPlatform.MYANIMELIST -> {
                val anime = myAnimeListClient.getAnimeById(id) ?: return 0
                anime.copy(episodes = myAnimeListClient.getEpisodesByMediaId(id))
            }
            MetadataPlatform.ANIDB ->
                aniDbClient.getAnimeByIds(listOf(id)).firstOrNull() ?: return 0
            MetadataPlatform.ANILIST -> return 0
        }
        if (external.episodes.isEmpty()) return 0

        val externalEpisodes = external.episodes.sortedWith(
            compareBy({ it.number ?: Int.MAX_VALUE }, { it.id })
        )

        var saved = 0
        ourEpisodes.forEachIndexed { index, ours ->
            // Number equality first, shifted by the assignment offset when the platform
            // numbers episodes per entry while we number them across the franchise. Rank
            // inside the season entry as fallback, but only for a dense one-to-one pairing:
            // same episode count and stored numbers inside the external range. A sparse
            // subset ranked against a full or partial entry would pair the wrong episodes
            // (a lone episode 32 is not the first episode of a series).
            val expected = ours.number + assignment.episodeOffset
            val byNumber = externalEpisodes.firstOrNull { it.number == expected }
            val byRank = if (assignment.seasons.size == 1 &&
                ourEpisodes.size == externalEpisodes.size &&
                ourEpisodes.all { it.number <= externalEpisodes.size }
            )
                externalEpisodes.getOrNull(index)
            else
                null
            val match = byNumber ?: byRank ?: return@forEachIndexed

            if (persist) {
                episodeMetadataLinkService.save(
                    EpisodeMetadataLink(
                        id = null, createdAt = null, updatedAt = null,
                        episodeId = ours.id,
                        platform = platform,
                        platformId = match.id,
                        platformAnimeId = external.id
                    )
                )
            }
            saved++
        }
        return saved
    }

    companion object {
        private const val MATCH_SCORE_THRESHOLD = 0.55
        private const val PREFERRED_ID_BONUS = 0.1
        private const val EXPAND_SCORE_THRESHOLD = 0.5
        private const val MAX_EXPANSIONS = 3
        private const val MAX_ANI_DB_CANDIDATES = 6
        private const val MAX_MAL_ENRICHMENTS = 5

        private val SEASON_PATTERNS = listOf(
            Regex("""(?i)\bseason\s*(\d{1,2})\b"""),
            Regex("""(?i)\b(\d{1,2})(?:st|nd|rd|th)\s+season\b"""),
            Regex("""第\s*(\d{1,2})\s*期"""),
            Regex("""(?<![\p{L}\p{N}])(\d{1,2})\s*期"""),
            Regex("""(?i)\bs(\d{1,2})\b"""),
        )
    }
}
