package fr.shikkanime.wrappers.impl.caches

import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.factories.AbstractAniDBWrapper
import fr.shikkanime.wrappers.impl.AniDBWrapper
import kotlinx.coroutines.runBlocking

object AniDBCachedWrapper : AbstractAniDBWrapper() {
    override suspend fun getAnimeTitles() = MapCache.getOrCompute(
        "AniDBCachedWrapper.getAnimeTitles",
        key = StringUtils.EMPTY_STRING
    ) { runBlocking { AniDBWrapper.getAnimeTitles() } }

    suspend fun getAnimeTitles(locale: String): Map<Int, Set<String>> {
        val language = locale.substringBefore("-")

        return getAnimeTitles().select("anime")
            .asSequence()
            .associate { anime ->
                val aid = anime.attr("aid").toInt()
                val titles = anime.select("title[xml:lang=\"$language\"],title[xml:lang=\"x-jat\"]")
                    .asSequence()
                    .map { it.text() }
                    .toSet()
                aid to titles
            }
            .filterValues { it.isNotEmpty() }
    }

    override suspend fun getAnimeDetails(clientId: String, animeId: Int) = MapCache.getOrCompute(
        "AniDBCachedWrapper.getAnimeDetails",
        key = "$clientId-$animeId"
    ) { runBlocking { AniDBWrapper.getAnimeDetails(clientId, animeId) } }

    override suspend fun getEpisodesByAnime(clientId: String, animeId: Int) = MapCache.getOrCompute(
        "AniDBCachedWrapper.getEpisodesByAnime",
        key = "$clientId-$animeId"
    ) { runBlocking { AniDBWrapper.getEpisodesByAnime(clientId, animeId) } }
}