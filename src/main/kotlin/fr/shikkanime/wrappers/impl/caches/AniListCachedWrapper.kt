package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.factories.AbstractAniListWrapper
import fr.shikkanime.wrappers.impl.AniListWrapper
import java.time.Duration
import kotlin.math.max

object AniListCachedWrapper : AbstractAniListWrapper() {
    private data class AniListCacheKey(val query: String, val page: Int, val limit: Int, val status: List<Status>)

    private val logger = LoggerFactory.getLogger(javaClass)
    private val defaultCacheDuration = Duration.ofDays(1)
    private const val HIGH_SIMILARITY_THRESHOLD = 0.9
    private const val LOW_SIMILARITY_THRESHOLD = 0.2
    private val TITLE_CLEANUP_REGEX = " \\(\\d{4}\\)$| \\(TV\\)$".toRegex()

    override suspend fun search(
        query: String,
        page: Int,
        limit: Int,
        status: List<Status>
    ) = MapCache.getOrComputeAsync(
        "AniListCachedWrapper.search",
        typeToken = object : TypeToken<MapCacheValue<Array<Media>>>() {},
        duration = defaultCacheDuration,
        key = AniListCacheKey(query, page, limit, status)
    ) { (query, page, limit, status) -> AniListWrapper.search(query, page, limit, status) }

    private suspend fun getLocalizedAnimeTitle(locale: String, animePlatform: AnimePlatform): String? {
        val platformId = animePlatform.platformId!!

        return runCatching {
            when(animePlatform.platform!!) {
                Platform.ANIM -> AnimationDigitalNetworkCachedWrapper.getShow(locale.split(StringUtils.DASH_STRING).last(), platformId.toInt()).originalTitle
                Platform.CRUN -> CrunchyrollCachedWrapper.getSeries(locale, platformId).title
                Platform.DISN -> DisneyPlusCachedWrapper.getShow(platformId).name
                Platform.NETF -> NetflixCachedWrapper.getShow(locale, platformId.toInt()).name
                Platform.PRIM -> PrimeVideoCachedWrapper.getShow(locale, platformId).name
                else -> null
            }
        }.getOrNull()
    }

    private fun normalizeTitle(title: String): String = title.replace(TITLE_CLEANUP_REGEX, StringUtils.EMPTY_STRING).trim()

    private suspend fun findMatchingAnime(
        animeName: String,
        firstReleasedYear: Int?,
        searchQuery: String = animeName
    ): MutableList<Media> {
        logger.config("Searching for '$searchQuery'...")
        val response = search(
            query = searchQuery,
            status = listOf(Status.RELEASING, Status.FINISHED),
            limit = 20
        )

        fun calculateScore(text: String): Double {
            val normalizedText = normalizeTitle(text)
            return max(
                StringUtils.similarity(searchQuery, normalizedText),
                StringUtils.similarity(animeName, normalizedText)
            )
        }

        return response.asSequence()
            .onEach { media ->
                with(media.title) {
                    romajiSearchSimilarity = calculateScore(romaji)
                    englishSearchSimilarity = english?.let { calculateScore(it) }
                    nativeSearchSimilarity = native?.let { calculateScore(it) }
                }

                media.isFirstReleasedYearRange = firstReleasedYear != null &&
                        media.startDate.year in (firstReleasedYear - 1)..(firstReleasedYear + 1)
            }
            .filter { media ->
                val isNotMusic = media.format != "MUSIC"
                val isRelevantResult = response.size <= 1 || media.title.maxSimilarity() >= LOW_SIMILARITY_THRESHOLD

                isNotMusic && isRelevantResult
            }
            .toMutableList()
    }

    suspend fun findAnilistMedia(animeName: String, platforms: List<AnimePlatform>?, firstReleasedYear: Int?): Media? {
        logger.config("Search on AniList with name: $animeName${firstReleasedYear?.let { " ($it)" } ?: ""}")

        var animeSearchResults = findMatchingAnime(animeName, firstReleasedYear)
        updateAnimeSearchResults(animeSearchResults, animeName, firstReleasedYear, platforms)
        processMediaRelations(animeSearchResults)

        logger.config("AniList API results (${animeSearchResults.size}):")
        animeSearchResults.forEach {
            logger.config("  - ID: ${it.id}, Format: ${it.format}")
            logger.config("      Title: ${it.title.romaji}, English: ${it.title.english}, Native: ${it.title.native}")
            logger.config("      Search similarity: romaji: ${it.title.romajiSearchSimilarity}, english: ${it.title.englishSearchSimilarity}, native: ${it.title.nativeSearchSimilarity}")
            logger.config("      Is within release year range: ${it.isFirstReleasedYearRange}")
            it.relations?.edges?.forEach { edge -> logger.config("        - Relation: ${edge.relationType} (${edge.node.format}) -> ${edge.node.id}") }
            logger.config("      Has parent relation in result list: ${it.hasParentRelation}")
        }

        // Find by the highest similarity
        val containsTvFormat = animeSearchResults.any { it.format == "TV" }
        animeSearchResults.singleOrNull { hasHighSimilarity(it) && !(it.hasParentRelation && it.format == "MOVIE" && containsTvFormat) }?.let { return it }

        // Find by who doesn't have a parent relation in the result list
        logger.config("-".repeat(100))
        animeSearchResults = animeSearchResults.filter { !it.hasParentRelation }.toMutableList()
        logger.config("AniList API results without parent relation (${animeSearchResults.size}):")
        animeSearchResults.forEach {
            logger.config("  - ID: ${it.id}, Format: ${it.format}")
            logger.config("      Title: ${it.title.romaji}, English: ${it.title.english}, Native: ${it.title.native}")
            logger.config("      Search similarity: romaji: ${it.title.romajiSearchSimilarity}, english: ${it.title.englishSearchSimilarity}, native: ${it.title.nativeSearchSimilarity}")
            logger.config("      Is within release year range: ${it.isFirstReleasedYearRange}")
        }

        animeSearchResults.singleOrNull()?.let { return it }
        animeSearchResults.singleOrNull { it.isFirstReleasedYearRange }?.let { return it }
        animeSearchResults.maxByOrNull { it.title.maxSimilarity() }?.let { return it }

        return null
    }

    private fun hasHighSimilarity(media: Media): Boolean = media.title.maxSimilarity() >= HIGH_SIMILARITY_THRESHOLD

    private fun isParentTvRelation(edge: RelationEdge): Boolean = edge.relationType == "PARENT" && edge.node.format == "TV"

    private fun isValidMatch(media: Media): Boolean = hasHighSimilarity(media) || (!media.hasParentRelation && media.isFirstReleasedYearRange)

    private suspend fun updateAnimeSearchResults(
        animeSearchResults: MutableList<Media>,
        animeName: String,
        firstReleasedYear: Int?,
        platforms: List<AnimePlatform>?
    ) {
        val first = animeSearchResults.singleOrNull()

        val hasParentTvRelation = first?.relations?.edges?.any(::isParentTvRelation) == true
        val hasSingleLowSimilarityParentRelation = first != null && !hasHighSimilarity(first) && first.format == "OVA" && hasParentTvRelation

        val hasUniqueHighSimilarityMatch = animeSearchResults.count(::hasHighSimilarity) == 1
        val hasUniqueValidMatch = animeSearchResults.count(::isValidMatch) == 1

        val isAlternativeTitleSearchNeeded = !platforms.isNullOrEmpty() && ((animeSearchResults.size <= 2 && !hasUniqueHighSimilarityMatch) || !hasUniqueValidMatch)

        val alternativeTitles = when {
            hasSingleLowSimilarityParentRelation -> listOfNotNull(first.relations.edges.first(::isParentTvRelation).node.title.english)
            isAlternativeTitleSearchNeeded -> platforms.mapNotNull { getLocalizedAnimeTitle("en-US", it) }
            else -> listOf()
        }.flatMap { it.split(" / ") }
            .toMutableSet()
            .filterNot { it.isBlank() || it.equals(animeName, true) }
            .takeIf { it.isNotEmpty() } ?: return

        val results: List<Media> = alternativeTitles.flatMap { findMatchingAnime(animeName, firstReleasedYear, it) }

        results.forEach { media ->
            if (hasSingleLowSimilarityParentRelation) {
                with(media.title) {
                    romajiSearchSimilarity = StringUtils.similarity(animeName, normalizeTitle(romaji))
                    englishSearchSimilarity = english?.let { StringUtils.similarity(animeName, normalizeTitle(it)) }
                    nativeSearchSimilarity = native?.let { StringUtils.similarity(animeName, normalizeTitle(it)) }
                }
            }

            val existingMedia = animeSearchResults.firstOrNull { it.id == media.id }

            if (existingMedia != null) {
                if (media.title.maxSimilarity() > existingMedia.title.maxSimilarity()) {
                    animeSearchResults.remove(existingMedia)
                    animeSearchResults.add(media)
                }
            } else {
                animeSearchResults.add(media)
            }
        }

        if (hasSingleLowSimilarityParentRelation) first.hasParentRelation = true
    }

    private fun processMediaRelations(medias: List<Media>) {
        val mediaIds = medias.map { it.id }.toSet()

        medias.forEach { media ->
            media.hasParentRelation = media.relations?.edges?.any { relation ->
                isParentRelation(media, relation, mediaIds)
            } == true
        }
    }

    private fun isParentRelation(media: Media, relation: RelationEdge, existingMediaIds: Set<Int>): Boolean {
        val node = relation.node
        val type = relation.relationType

        // Règle 1 : Relation directe (Parent/Prequel) présente dans la liste courante avec un ID inférieur
        val isSequentialPrequel = type in listOf("PARENT", "PREQUEL") &&
                node.id in existingMediaIds &&
                node.id < media.id &&
                !(media.format == "MOVIE" && node.format == "MOVIE")

        // Règle 2 : Prequel spécifique aux films
        val isMoviePrequel = type == "PREQUEL" &&
                media.format == "MOVIE" &&
                node.format in listOf("MOVIE", "ONA")

        // Règle 3 : Spinoff basé sur un personnage (sauf si le média courant est TV/Film)
        val isCharacterSideStory = type == "CHARACTER" &&
                media.format !in listOf("TV", "MOVIE") &&
                node.format == "TV"

        // Règle 4 : Histoires alternatives
        val isAlternativeContext = type in listOf("ALTERNATIVE", "OTHER") &&
                media.format !in listOf("TV", "ONA") &&
                node.format == "TV"

        return isSequentialPrequel || isMoviePrequel || isCharacterSideStory || isAlternativeContext
    }
}