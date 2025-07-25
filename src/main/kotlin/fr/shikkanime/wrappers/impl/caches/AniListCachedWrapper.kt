package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.factories.AbstractAniListWrapper
import fr.shikkanime.wrappers.impl.AniListWrapper
import java.time.Duration
import kotlin.math.max

object AniListCachedWrapper : AbstractAniListWrapper() {
    private data class AniListCacheKey(val query: String, val page: Int, val limit: Int, val status: List<Status>)

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

        return when(animePlatform.platform!!) {
            Platform.ANIM -> AnimationDigitalNetworkCachedWrapper.getShow(locale.split(StringUtils.DASH_STRING).last(), platformId.toInt()).originalTitle
            Platform.CRUN -> CrunchyrollCachedWrapper.getSeries(locale, platformId).title
            Platform.DISN -> DisneyPlusCachedWrapper.getShow(platformId).name
            Platform.NETF -> NetflixCachedWrapper.getShow(locale, platformId.toInt()).name
            Platform.PRIM -> PrimeVideoCachedWrapper.getShow(locale, platformId).name
        }
    }

    private fun normalizeTitle(title: String): String = title.replace(TITLE_CLEANUP_REGEX, StringUtils.EMPTY_STRING).trim()

    suspend fun findAnilistMedia(animeName: String, platforms: List<AnimePlatform>?, firstReleasedYear: Int?): Media? {
        println("Search on AniList with name: $animeName${firstReleasedYear?.let { " ($it)" } ?: ""}")

        val titleMediaSearch: suspend (String) -> MutableList<Media> = { title ->
            println("Searching for '$title'...")

            val response = search(
                query = title,
                status = listOf(Status.RELEASING, Status.FINISHED),
                limit = 20
            )

            response.asSequence()
                .onEach { media ->
                    media.title.apply {
                        romajiSearchSimilarity = max(StringUtils.similarity(title, normalizeTitle(romaji)), StringUtils.similarity(animeName, normalizeTitle(romaji)))
                        english?.let { englishSearchSimilarity = max(StringUtils.similarity(title, normalizeTitle(it)), StringUtils.similarity(animeName, normalizeTitle(it))) }
                        native?.let { nativeSearchSimilarity = max(StringUtils.similarity(title, normalizeTitle(it)), StringUtils.similarity(animeName, normalizeTitle(it))) }
                    }

                    media.isFirstReleasedYearRange = firstReleasedYear != null && media.startDate.year in (firstReleasedYear - 1)..(firstReleasedYear + 1)
                }
                .filter { it.format != "MUSIC" && (response.size <= 1 || it.title.maxSimilarity() >= LOW_SIMILARITY_THRESHOLD) }
                .apply {
                    val medias = this

                    medias.forEach { media ->
                        media.relations?.edges?.let { relationEdges -> media.hasParentRelation = relationEdges.any { relation ->
                            ((relation.relationType in listOf("PARENT", "PREQUEL")) && medias.any { it.id == relation.node.id } && relation.node.id < media.id) ||
                                    (relation.relationType == "PREQUEL" && media.format == "MOVIE" && relation.node.format in listOf("MOVIE", "ONA")) ||
                                    (relation.relationType == "CHARACTER" && media.format !in listOf("TV", "MOVIE") && relation.node.format == "TV") ||
                                    (relation.relationType in listOf("ALTERNATIVE", "OTHER") && media.format !in listOf("TV", "ONA") && relation.node.format == "TV")
                        } }
                    }
                }.toMutableList()
        }

        var animeSearchResults = titleMediaSearch(animeName)

        println("AniList API results UNEDITED (${animeSearchResults.size}):")
        animeSearchResults.forEach {
            println("  - ID: ${it.id}, Format: ${it.format}")
            println("      Title: ${it.title.romaji}, English: ${it.title.english}, Native: ${it.title.native}")
            println("      Search similarity: romaji: ${it.title.romajiSearchSimilarity}, english: ${it.title.englishSearchSimilarity}, native: ${it.title.nativeSearchSimilarity}")
            println("      Is within release year range: ${it.isFirstReleasedYearRange}")
            it.relations?.edges?.forEach { edge -> println("        - Relation: ${edge.relationType} (${edge.node.format}) -> ${edge.node.id}") }
            println("      Has parent relation in result list: ${it.hasParentRelation}")
        }

        val first = animeSearchResults.firstOrNull()
        if (animeSearchResults.size == 1 && first!!.title.maxSimilarity() < HIGH_SIMILARITY_THRESHOLD && first.format == "OVA" && first.relations?.edges?.any { it.relationType == "PARENT" && it.node.format == "TV" } == true) {
            // Search with the parent relation
            val subSearch: List<Media> = titleMediaSearch(first.relations.edges.first { it.relationType == "PARENT" && it.node.format == "TV" }.node.title.english!!)

            subSearch.forEach { media ->
                media.title.apply {
                    romajiSearchSimilarity = StringUtils.similarity(animeName, normalizeTitle(romaji))
                    english?.let { englishSearchSimilarity = StringUtils.similarity(animeName, normalizeTitle(it)) }
                    native?.let { nativeSearchSimilarity = StringUtils.similarity(animeName, normalizeTitle(it)) }
                }

                if (animeSearchResults.any { it.id == media.id }) {
                    // Better similarity
                    val mediaInList = animeSearchResults.first { it.id == media.id }

                    if (media.title.maxSimilarity() > mediaInList.title.maxSimilarity()) {
                        animeSearchResults.remove(mediaInList)
                        animeSearchResults.add(media)
                    }
                } else {
                    animeSearchResults.add(media)
                }
            }

            first.hasParentRelation = true
        } else if (((animeSearchResults.size <= 2 && animeSearchResults.singleOrNull { it.title.maxSimilarity() >= HIGH_SIMILARITY_THRESHOLD } == null) || animeSearchResults.singleOrNull { it.title.maxSimilarity() >= HIGH_SIMILARITY_THRESHOLD || (!it.hasParentRelation && it.isFirstReleasedYearRange) } == null) && !platforms.isNullOrEmpty()) {
            println("Few results found, checking alternative titles from platforms...")
            platforms.forEach {
                val title = getLocalizedAnimeTitle("en-US", it) ?: return@forEach
                if (title.isBlank() || title.equals(animeName, true)) return@forEach
                val subSearch: List<Media> = titleMediaSearch(title)

                subSearch.forEach { media ->
                    if (animeSearchResults.any { it.id == media.id }) {
                        // Better similarity
                        val mediaInList = animeSearchResults.first { it.id == media.id }

                        if (media.title.maxSimilarity() > mediaInList.title.maxSimilarity()) {
                            animeSearchResults.remove(mediaInList)
                            animeSearchResults.add(media)
                        }
                    } else {
                        animeSearchResults.add(media)
                    }
                }
            }
        }

        println("AniList API results (${animeSearchResults.size}):")
        animeSearchResults.forEach {
            println("  - ID: ${it.id}, Format: ${it.format}")
            println("      Title: ${it.title.romaji}, English: ${it.title.english}, Native: ${it.title.native}")
            println("      Search similarity: romaji: ${it.title.romajiSearchSimilarity}, english: ${it.title.englishSearchSimilarity}, native: ${it.title.nativeSearchSimilarity}")
            println("      Is within release year range: ${it.isFirstReleasedYearRange}")
            it.relations?.edges?.forEach { edge -> println("        - Relation: ${edge.relationType} (${edge.node.format}) -> ${edge.node.id}") }
            println("      Has parent relation in result list: ${it.hasParentRelation}")
        }

        // Find by the highest similarity
        animeSearchResults.singleOrNull { it.title.maxSimilarity() >= HIGH_SIMILARITY_THRESHOLD }?.let { return it }

        // Find by who doesn't have a parent relation in the result list
        println("-".repeat(100))
        animeSearchResults = animeSearchResults.filter { !it.hasParentRelation }.toMutableList()
        println("AniList API results without parent relation (${animeSearchResults.size}):")
        animeSearchResults.forEach {
            println("  - ID: ${it.id}, Format: ${it.format}")
            println("      Title: ${it.title.romaji}, English: ${it.title.english}, Native: ${it.title.native}")
            println("      Search similarity: romaji: ${it.title.romajiSearchSimilarity}, english: ${it.title.englishSearchSimilarity}, native: ${it.title.nativeSearchSimilarity}")
            println("      Is within release year range: ${it.isFirstReleasedYearRange}")
        }

        animeSearchResults.singleOrNull()?.let { return it }
        animeSearchResults.singleOrNull { it.isFirstReleasedYearRange }?.let { return it }
        animeSearchResults.maxByOrNull { it.title.maxSimilarity() }?.let { return it }

        return null
    }
}