package fr.shikkanime.wrappers

import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.factories.AbstractMyAnimeListWrapper
import fr.shikkanime.wrappers.impl.caches.AniDBCachedWrapper
import fr.shikkanime.wrappers.impl.caches.AniListCachedWrapper
import fr.shikkanime.wrappers.impl.caches.AnimeNewsNetworkCachedWrapper
import fr.shikkanime.wrappers.impl.caches.MyAnimeListCachedWrapper
import java.time.LocalDate

class EpisodeConnectorWrapper {
    data class AiringDate(
        val date: LocalDate,
        var occurrenceCount: Int = 1
    )

    data class Source(
        val platform: String,
        val url: String?
    )

    data class AggregatedEpisode(
        val titles: MutableSet<String>,
        val airings: MutableSet<AiringDate>,
        val externalIds: MutableSet<String>,
        val sources: MutableSet<Source>
    ) {
        constructor(titles: Set<String>, date: LocalDate, platform: String, id: String, url: String? = null) : this(
            titles.toMutableSet(),
            mutableSetOf(AiringDate(date)),
            mutableSetOf(id),
            mutableSetOf(Source(platform, url))
        )

        fun addAiring(date: LocalDate) {
            val existing = airings.find { it.date == date }
            if (existing == null) {
                airings.add(AiringDate(date))
            } else {
                existing.occurrenceCount++
            }
        }

        fun merge(newTitles: Set<String>, date: LocalDate, platform: String, id: String, url: String? = null) {
            titles.addAll(newTitles)
            addAiring(date)
            externalIds.add(id)
            sources.add(Source(platform, url))
        }

        val sourcePlatforms: Set<String> get() = sources.map { it.platform }.toSet()
    }

    private val aggregatedEpisodes = mutableListOf<AggregatedEpisode>()

    private fun findMatchingEpisode(titles: Set<String>, platform: String, id: String): AggregatedEpisode? {
        return aggregatedEpisodes.find { episode ->
            val isSameId = episode.externalIds.contains(id)
            val hasSimilarTitle = titles.any { title ->
                episode.titles.any { StringUtils.similarity(it, title) >= 0.8 }
            }

            (isSameId || hasSimilarTitle) && !episode.sourcePlatforms.contains(platform)
        }
    }

    fun addEpisodeSource(titles: Set<String>, date: LocalDate, platform: String, id: String, url: String? = null) {
        val existing = findMatchingEpisode(titles, platform, id)

        if (existing != null) {
            existing.merge(titles, date, platform, id, url)
        } else {
            aggregatedEpisodes.add(AggregatedEpisode(titles, date, platform, id, url))
        }
    }

    private fun extractIdFromUrl(url: String?): Int? {
        val idRegex = "a?id=(\\d*)$".toRegex()
        return url?.let { idRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() }
    }

    suspend fun fetchAndAggregate(name: String, platforms: List<AnimePlatform>): List<AggregatedEpisode> {
        val anilistMedia = AniListCachedWrapper.findAnilistMedia(name, platforms, null) ?: return emptyList()
        val malId = anilistMedia.idMal ?: return emptyList()

        val myAnimeListMedia = MyAnimeListCachedWrapper.getMediaById(malId)
        val myAnimeListEpisodes = MyAnimeListCachedWrapper.getEpisodesByMediaId(malId)

        // Aggregation de AniDB
        val aniDBUrl = myAnimeListMedia.external.singleOrNull { it.name == "AniDB" }?.url
        extractIdFromUrl(aniDBUrl)?.let { id ->
            AniDBCachedWrapper.getEpisodesByMediaId(id).forEach {
                addEpisodeSource(it.titles, it.aired, "AniDB", it.id, aniDBUrl)
            }
        }

        // Aggregation de AnimeNewsNetwork
        val annUrl = myAnimeListMedia.external.singleOrNull { it.name == "ANN" }?.url
        extractIdFromUrl(annUrl)?.let { id ->
            val annEpisodes = AnimeNewsNetworkCachedWrapper.getEpisodesByMediaId(id)

            if (annEpisodes.isEmpty()) {
                val media = AnimeNewsNetworkCachedWrapper.getMediaById(id)
                addEpisodeSource(media.titles, media.airedFrom, "ANN", "1", annUrl)
            } else {
                annEpisodes.forEach {
                    addEpisodeSource(it.titles, it.aired, "ANN", it.id, annUrl)
                }
            }
        }

        // Aggregation de MyAnimeList
        val malUrl = "https://myanimelist.net/anime/$malId"
        if (myAnimeListEpisodes.isEmpty()) {
            val titles = myAnimeListMedia.titles.map(AbstractMyAnimeListWrapper.Title::title).toSet()
            addEpisodeSource(titles, myAnimeListMedia.aired.from.toLocalDate(), "MyAnimeList", "1", malUrl)
        } else {
            myAnimeListEpisodes.forEach { malEp ->
                val titles = setOfNotNull(malEp.title, malEp.titleJapanese, malEp.titleRomanji)
                addEpisodeSource(titles, malEp.aired.toLocalDate(), "MyAnimeList", malEp.id.toString(), malUrl)
            }
        }

        return aggregatedEpisodes
    }
}