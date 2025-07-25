package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.factories.AbstractAniListWrapper
import fr.shikkanime.wrappers.impl.AniListWrapper
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.ZonedDateTime

object AniListCachedWrapper : AbstractAniListWrapper() {
    private data class AniListCacheKey(val query: String, val page: Int, val limit: Int, val status: List<Status>)

    private val defaultCacheDuration = Duration.ofDays(1)
    private const val HIGH_SIMILARITY_THRESHOLD = 0.95
    private const val LOW_SIMILARITY_THRESHOLD = 0.2
    private val TITLE_CLEANUP_REGEX = " \\(\\d{4}\\)$| \\(TV\\)$".toRegex()

    override suspend fun search(
        query: String,
        page: Int,
        limit: Int,
        status: List<Status>
    ) = MapCache.getOrCompute(
        "AniListCachedWrapper.search",
        typeToken = object : TypeToken<MapCacheValue<Array<Media>>>() {},
        duration = defaultCacheDuration,
        key = AniListCacheKey(query, page, limit, status)
    ) { (query, page, limit, status) -> runBlocking { AniListWrapper.search(query, page, limit, status) } }

    private suspend fun getLocalizedAnimeTitle(locale: String, animePlatform: AnimePlatform): String? {
        val platformId = animePlatform.platformId!!

        return when(animePlatform.platform!!) {
            Platform.ANIM -> AnimationDigitalNetworkCachedWrapper.getShow(platformId.toInt()).originalTitle
            Platform.CRUN -> CrunchyrollCachedWrapper.getSeries(locale, platformId).title
            Platform.DISN -> DisneyPlusCachedWrapper.getShow(platformId).name
            Platform.NETF -> NetflixCachedWrapper.getShow(locale, platformId.toInt()).name
            Platform.PRIM -> PrimeVideoCachedWrapper.getShow(locale, platformId).name
        }
    }

    private fun normalizeTitle(title: String): String = title.replace(TITLE_CLEANUP_REGEX, "")

    private fun calculateSimilarity(name: String, media: Media): Double {
        val mediaTitle = media.title.english ?: media.title.romaji
        return StringUtils.similarity(name, normalizeTitle(mediaTitle))
    }

    private fun hasHighSimilarityMatch(name: String, medias: List<Media>, currentMedia: Media): Boolean {
        return medias.none { 
            it.id != currentMedia.id && calculateSimilarity(name, it) >= HIGH_SIMILARITY_THRESHOLD 
        }
    }

    private fun shouldFilterByCharacterRelation(media: Media): Boolean {
        val characterRelation = media.relations?.edges?.find { it.relationType == "CHARACTER" }
        val hasCharacterRelation = characterRelation != null && characterRelation.node.format == "TV"
        println("hasCharacterRelation: $hasCharacterRelation")
        return hasCharacterRelation
    }

    private fun shouldFilterByParentRelation(media: Media, animeSearchResults: List<Media>): Boolean {
        val parentRelation = media.relations?.edges?.find { it.relationType == "PARENT" }
        val hasParent = parentRelation != null && animeSearchResults.any { it.id == parentRelation.node.id }
        val isParentSpecial = parentRelation != null && parentRelation.node.format == "SPECIAL"
        println("hasParent: $hasParent, isParentSpecial: $isParentSpecial")
        return hasParent || isParentSpecial
    }

    private fun shouldFilterByOtherRelation(media: Media, animeSearchResults: List<Media>): Boolean {
        val otherRelation = media.relations?.edges?.find { it.relationType == "OTHER" }?.takeIf { it.node.id < media.id }
        val hasOther = otherRelation != null && animeSearchResults.any { it.id == otherRelation.node.id }
        println("hasOther: $hasOther")
        return hasOther
    }

    private fun shouldFilterByPrequelRelation(media: Media, medias: List<Media>): Boolean {
        val tvPrequelRelation = media.relations?.edges?.find { it.relationType == "PREQUEL" }
        val hasTVPrequel = tvPrequelRelation != null && medias.any { it.id == tvPrequelRelation.node.id && it.format == "TV" }
        val hasSpecialPrequel = media.format != "TV" && tvPrequelRelation != null && tvPrequelRelation.node.format in listOf("SPECIAL", "OVA")
        println("hasTVPrequel: $hasTVPrequel, hasSpecialPrequel: $hasSpecialPrequel")
        
        if (hasTVPrequel || hasSpecialPrequel) return true
        
        val prequelRelations = media.relations?.edges?.filter { it.relationType == "PREQUEL" }
        val hasPrequelInMedias = prequelRelations?.any { prequel -> medias.any { it.id == prequel.node.id } } ?: false
        println("hasPrequelInMedias: $hasPrequelInMedias")
        return hasPrequelInMedias
    }

    private fun shouldFilterBySequelRelation(media: Media, medias: List<Media>): Boolean {
        val tvSequelRelation = media.relations?.edges?.find { it.relationType == "SEQUEL" && it.node.format == "TV" }
        val hasTVSequel = tvSequelRelation != null && medias.any { it.id == tvSequelRelation.node.id }
        println("hasTVSequel: $hasTVSequel")
        return media.format != "TV" && hasTVSequel
    }

    private fun shouldFilterByAlternativeRelation(media: Media, medias: List<Media>, firstReleasedYear: Int?): Boolean {
        val alternativeTVRelations = media.relations?.edges?.filter { it.relationType == "ALTERNATIVE" && it.node.format == "TV" }
        val hasAlternativeTV = alternativeTVRelations?.any { alternative -> 
            alternative.node.id < media.id && 
            medias.any { it.id == alternative.node.id } && 
            alternative.node.title.native.equals(media.title.romaji.replace(" ($firstReleasedYear)", ""), true) 
        } ?: false
        println("hasAlternativeTV: $hasAlternativeTV")
        return hasAlternativeTV
    }

    private fun shouldFilterByReleaseYear(media: Media, firstReleasedYear: Int?): Boolean {
        val isWithinReleaseYearRange = firstReleasedYear != null && media.startDate.year !in (firstReleasedYear - 1)..(firstReleasedYear + 1)
        println("isWithinReleaseYearRange: $isWithinReleaseYearRange (media year: ${media.startDate.year}, firstReleasedYear: $firstReleasedYear)")
        return isWithinReleaseYearRange
    }

    private suspend fun getAlternativeTitles(platforms: List<AnimePlatform>): Set<String> {
        return platforms.mapNotNull { runCatching { getLocalizedAnimeTitle("en-US", it) }.getOrNull() }.toSet()
    }

    private fun shouldSearchWithAlternativeTitles(
        platforms: List<AnimePlatform>?,
        animeSearchResults: List<Media>
    ): Boolean {
        if (platforms.isNullOrEmpty()) return false
        if (animeSearchResults.isEmpty()) return true
        return animeSearchResults.none { it.externalLinks?.any { link -> link.type == "STREAMING" } == true }
    }

    private suspend fun searchWithAlternativeTitles(
        platforms: List<AnimePlatform>,
        titleMediaSearch: suspend (String) -> List<Media>
    ): Pair<String, List<Media>>? {
        val titles = getAlternativeTitles(platforms)
        println("Try to find with another title: $titles")
        if (titles.isEmpty()) return null

        val titleAssociates = titles.associateWith { title ->
            println("Try to find with title: $title")
            titleMediaSearch(title)
        }

        val firstNonEmpty = titleAssociates.entries.firstOrNull { it.value.isNotEmpty() }
        return firstNonEmpty?.let { it.key to it.value }
    }

    private suspend fun tryAlternativeTitlesRecursive(
        platforms: List<AnimePlatform>,
        firstReleasedYear: Int?,
        latestReleaseDateTime: ZonedDateTime,
        depth: Int
    ): Media? {
        val titles = getAlternativeTitles(platforms)
        println("Try to find with another title: $titles")
        if (titles.isEmpty()) return null

        return titles.firstNotNullOfOrNull { title ->
            findAnilistMedia(title, platforms, firstReleasedYear, latestReleaseDateTime, depth - 1)
        }
    }

    suspend fun findAnilistMedia(animeName: String, platforms: List<AnimePlatform>?, firstReleasedYear: Int?, latestReleaseDateTime: ZonedDateTime, depth: Int = 3): Media? {
        println("========================================")
        println("[START] Finding AniList media")
        if (depth <= 0) {
            println("[DEPTH LIMIT] Recursion depth limit reached, returning null")
            return null
        }
        var name = animeName
        println("[INPUT] Anime: $name")
        platforms?.forEach { println("[INPUT] Platform: ${it.platform} - ${it.platformId}") }
        println("[INPUT] First released year: $firstReleasedYear")
        println("[INPUT] Latest release date time: $latestReleaseDateTime")

        val isReleasing = latestReleaseDateTime.isAfter(ZonedDateTime.now().minusWeeks(2))
        println("[STATUS] Is releasing: $isReleasing (based on latest episode release: $latestReleaseDateTime)")
        
        val titleMediaSearch: suspend (String) -> List<Media> = { title ->
            val status = if (isReleasing) listOf(Status.RELEASING, Status.FINISHED) else listOf(Status.FINISHED)
            println("[SEARCH] Searching for '$title' with status: $status")
            val results = search(
                query = title,
                status = status,
                limit = 20
            ).filter { it.format != "MUSIC" }
            println("[SEARCH] Found ${results.size} results (after filtering MUSIC format)")
            results
        }

        var animeSearchResults = titleMediaSearch(name)
        println("[RESULTS] Initial anime search results on AniList (${animeSearchResults.size} results):")
        animeSearchResults.forEach { println("  - ID: ${it.id}, Title: ${it.title.romaji}, Format: ${it.format}") }

        if (shouldSearchWithAlternativeTitles(platforms, animeSearchResults)) {
            println("[ALTERNATIVE] Checking alternative titles from platforms...")
            val alternativeResult = searchWithAlternativeTitles(platforms!!, titleMediaSearch)
            
            if (alternativeResult != null) {
                name = alternativeResult.first
                animeSearchResults = alternativeResult.second
                println("[ALTERNATIVE] Found with alternative title: '$name' (${animeSearchResults.size} results)")
            } else {
                println("[ALTERNATIVE] No results found with alternative titles, continuing with original search results")
            }
        }

        val medias = animeSearchResults.filter { it.externalLinks?.any { link -> link.type == "STREAMING" } == true }
        println("[FILTER] Filtering medias with STREAMING links: ${medias.size} out of ${animeSearchResults.size}")
        
        // If no streaming links found but we have platforms, use all search results for filtering
        val mediasToFilter = if (medias.isEmpty() && !platforms.isNullOrEmpty()) {
            println("[FILTER] No streaming links found, but platforms provided - using all search results for filtering")
            animeSearchResults
        } else {
            medias
        }

        val filteredMedias = mediasToFilter.sortedBy { it.id }.filter { media ->
            println("[FILTER] Checking media ID ${media.id}: ${media.title.romaji}")

            val similarity = calculateSimilarity(name, media)
            println("[FILTER]   Similarity: $similarity")

            // If only one media with a similarity of 95% or more, return it
            if (hasHighSimilarityMatch(name, mediasToFilter, media)) {
                when {
                    similarity > HIGH_SIMILARITY_THRESHOLD -> {
                        println("[FILTER]   ✓ High similarity match (>${HIGH_SIMILARITY_THRESHOLD}), PASSED")
                        return@filter true
                    }
                    similarity < LOW_SIMILARITY_THRESHOLD -> {
                        println("[FILTER]   ✗ Low similarity (<${LOW_SIMILARITY_THRESHOLD}), REJECTED")
                        return@filter false
                    }
                }
            }

            // Apply various relation-based filters
            when {
                shouldFilterByCharacterRelation(media) -> {
                    println("[FILTER]   ✗ Filtered by CHARACTER relation, REJECTED")
                    return@filter false
                }
                shouldFilterByParentRelation(media, animeSearchResults) -> {
                    println("[FILTER]   ✗ Filtered by PARENT relation, REJECTED")
                    return@filter false
                }
                shouldFilterByOtherRelation(media, animeSearchResults) -> {
                    println("[FILTER]   ✗ Filtered by OTHER relation, REJECTED")
                    return@filter false
                }
                shouldFilterByPrequelRelation(media, mediasToFilter) -> {
                    println("[FILTER]   ✗ Filtered by PREQUEL relation, REJECTED")
                    return@filter false
                }
                shouldFilterBySequelRelation(media, mediasToFilter) -> {
                    println("[FILTER]   ✗ Filtered by SEQUEL relation, REJECTED")
                    return@filter false
                }
                shouldFilterByAlternativeRelation(media, mediasToFilter, firstReleasedYear) -> {
                    println("[FILTER]   ✗ Filtered by ALTERNATIVE relation, REJECTED")
                    return@filter false
                }
                shouldFilterByReleaseYear(media, firstReleasedYear) -> {
                    println("[FILTER]   ✗ Filtered by release year, REJECTED")
                    return@filter false
                }
            }

            println("[FILTER]   ✓ All filters passed, ACCEPTED")
            true
        }.distinctBy { it.id }

        println("[SUMMARY] Total search results: ${animeSearchResults.size}")
        println("[SUMMARY] Medias with streaming links: ${medias.size}")
        println("[SUMMARY] Medias used for filtering: ${mediasToFilter.size}")
        println("[SUMMARY] Filtered medias after all checks: ${filteredMedias.size}")
        filteredMedias.forEach { println("  - ID: ${it.id}, Title: ${it.title.romaji}") }

        println("[MATCHING] Attempting to find best match...")
        val firstPassMedia = findMatchingMedia(name, firstReleasedYear, mediasToFilter, filteredMedias, animeSearchResults)

        if (firstPassMedia != null) {
            println("[RESULT] ✓ Found media: ID ${firstPassMedia.id}, Title: ${firstPassMedia.title.romaji}")
            return firstPassMedia
        }
        
        if (platforms.isNullOrEmpty()) {
            println("[RESULT] ✗ No media found and no platforms provided, returning null")
            return null
        }

        println("[RECURSIVE] Trying alternative titles with recursion (depth: ${depth - 1})...")
        val recursiveResult = tryAlternativeTitlesRecursive(platforms, firstReleasedYear, latestReleaseDateTime, depth)
        
        if (recursiveResult != null) {
            println("[RESULT] ✓ Found via recursive search: ID ${recursiveResult.id}, Title: ${recursiveResult.title.romaji}")
        } else {
            println("[RESULT] ✗ No media found after all attempts, returning null")
        }
        
        return recursiveResult
    }

    private fun buildSearchNames(name: String, firstReleasedYear: Int?): Set<String> {
        return buildSet {
            add(name)
            firstReleasedYear?.takeIf { !name.contains(it.toString()) }?.let { add("$name ($it)") }
        }
    }

    private fun createTitleMatcher(searchNames: Set<String>): (Media) -> Boolean {
        return { media ->
            val mediaTitles = listOfNotNull(
                media.title.romaji,
                media.title.english,
                media.title.native
            )
            mediaTitles.any { mediaTitle -> 
                searchNames.any { searchName -> 
                    mediaTitle.equals(searchName, ignoreCase = true) 
                } 
            }
        }
    }

    private fun findMatchingMedia(
        name: String,
        firstReleasedYear: Int?,
        medias: List<Media>,
        filteredMedias: List<Media>,
        animeSearchResults: List<Media>
    ): Media? {
        val searchNames = buildSearchNames(name, firstReleasedYear)
        println("[MATCHING] Search names: $searchNames")
        val titleMatcher = createTitleMatcher(searchNames)

        // Try exact match in filtered medias or single filtered media
        println("[MATCHING] Strategy 1: Exact match in filtered medias (${filteredMedias.size} candidates)")
        filteredMedias.singleOrNull(titleMatcher)?.let { 
            println("[MATCHING] ✓ Found exact title match in filtered medias: ID ${it.id}")
            return it 
        }
        
        if (filteredMedias.size == 1) {
            println("[MATCHING] ✓ Only one filtered media, returning it: ID ${filteredMedias.first().id}")
            return filteredMedias.first()
        }

        // Fallback to single media in unfiltered results
        println("[MATCHING] Strategy 2: Single media in unfiltered results (medias: ${medias.size}, filtered: ${filteredMedias.size})")
        if (filteredMedias.isEmpty() && medias.size == 1) {
            val result = animeSearchResults.firstOrNull()
            println("[MATCHING] ✓ Returning first anime search result: ID ${result?.id}")
            return result
        }

        // Fallback to single anime search result
        println("[MATCHING] Strategy 3: Single anime search result (${animeSearchResults.size} results)")
        if (filteredMedias.isEmpty() && medias.isEmpty() && animeSearchResults.size == 1) {
            println("[MATCHING] ✓ Only one search result, returning it: ID ${animeSearchResults.first().id}")
            return animeSearchResults.first()
        }

        // Try exact match in all anime search results
        println("[MATCHING] Strategy 4: Exact match in all anime search results (${animeSearchResults.size} candidates)")
        val finalMatch = animeSearchResults.singleOrNull(titleMatcher)
        if (finalMatch != null) {
            println("[MATCHING] ✓ Found exact title match in all results: ID ${finalMatch.id}")
        } else {
            println("[MATCHING] ✗ No exact match found in any strategy")
        }
        return finalMatch
    }
}