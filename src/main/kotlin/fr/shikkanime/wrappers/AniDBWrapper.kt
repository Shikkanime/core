package fr.shikkanime.wrappers

import fr.shikkanime.utils.FileManager
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.PersistentMapCache
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.impl.caches.CrunchyrollCachedWrapper
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.Duration

object AniDBWrapper {
    private val httpRequest = HttpRequest()

    @Synchronized
    fun getAnimeTitles(): Document {
        PersistentMapCache.get<String>("anime-titles")?.let { cachedValue ->
            if (Duration.ofMillis(System.currentTimeMillis() - cachedValue.timestamp).toDays() < 1) {
                return Jsoup.parse(cachedValue.value as String)
            }
        }

        return synchronized(AniDBWrapper) { runBlocking {
            val response = httpRequest.get("https://anidb.net/api/anime-titles.xml.gz")
            require(response.status == HttpStatusCode.OK) { "Failed to get anime titles" }
            Jsoup.parse(String(FileManager.decompressGzip(response.bodyAsBytes())))
        } }.also { PersistentMapCache.put("anime-titles", it.toString()) }
    }

    private fun levenshteinDistance(source: String, target: String): Int {
        val filteredCharacters = setOf('-')
        val src = source.lowercase().filterNot { it in filteredCharacters }
        val tgt = target.lowercase().filterNot { it in filteredCharacters }

        val m = src.length
        val n = tgt.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (src[i - 1] == tgt[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[m][n]
    }

    private fun toRomanNumber(number: Int): String {
        val romanNumbers = mapOf(
            1000 to "M",
            900 to "CM",
            500 to "D",
            400 to "CD",
            100 to "C",
            90 to "XC",
            50 to "L",
            40 to "XL",
            10 to "X",
            9 to "IX",
            5 to "V",
            4 to "IV",
            1 to "I"
        )

        var n = number
        val romanNumber = StringBuilder()

        for ((value, roman) in romanNumbers) {
            while (n >= value) {
                romanNumber.append(roman)
                n -= value
            }
        }

        return romanNumber.toString()
    }

    private suspend fun getSeriesTitlesWithEpisodeId(locale: String, episodeId: String): Set<String> {
        var crunchyrollEpisode = CrunchyrollCachedWrapper.getEpisode(locale, episodeId)

        if (crunchyrollEpisode.seasonTitle.contains("(Mature)")) {
            val season = CrunchyrollCachedWrapper.getSeason(locale, crunchyrollEpisode.seasonId)
            val identifierNotMature = season.identifier.removeSuffix("-M")
            val seasons = CrunchyrollCachedWrapper.getSeasonsBySeriesId(locale, crunchyrollEpisode.seriesId)

            seasons.find { it.identifier == identifierNotMature }?.let { originalSeason ->
                CrunchyrollCachedWrapper.getEpisodesBySeasonId(locale, originalSeason.id)
                    .find { it.identifier == crunchyrollEpisode.identifier.replace("-M", "") }
                    ?.let { crunchyrollEpisode = it }
            }
        }

        if (crunchyrollEpisode.versions?.find { it.guid == crunchyrollEpisode.id!! }?.original == false) {
            crunchyrollEpisode = CrunchyrollCachedWrapper.getEpisode(
                locale,
                crunchyrollEpisode.versions.find { it.original }?.guid ?: crunchyrollEpisode.id!!
            )
        }

        val seasonEpisodes = CrunchyrollCachedWrapper.getEpisodesBySeasonId(locale, crunchyrollEpisode.seasonId)

        return buildSet {
            if (!crunchyrollEpisode.seasonDisplayNumber.isNullOrBlank() &&
                crunchyrollEpisode.seasonDisplayNumber != "1" &&
                !crunchyrollEpisode.seriesTitle.endsWith(crunchyrollEpisode.seasonDisplayNumber)
            ) {
                add("${crunchyrollEpisode.seriesTitle} Season ${crunchyrollEpisode.seasonDisplayNumber}")
            }

            val season = crunchyrollEpisode.seasonTitle.replace("(- Broadcast Version|Short Animation Series|\\(Japanese Audio\\)|\\(French Dub\\)|\\(French / German Europe\\))$".toRegex(), "").trim()
            val minZonedDateTime = seasonEpisodes.minOf { it.premiumAvailableDate }

            if (crunchyrollEpisode.premiumAvailableDate.year == minZonedDateTime.year && seasonEpisodes.count { it.premiumAvailableDate == minZonedDateTime } > 5 && !season.contains(minZonedDateTime.year.toString())) {
                add("$season (${minZonedDateTime.year})")
            }

            add(season)

            crunchyrollEpisode.seasonDisplayNumber?.toIntOrNull()?.let { toRomanNumber(it) }?.let { romanNumber ->
                if (romanNumber != "I" && !crunchyrollEpisode.seriesTitle.endsWith(romanNumber)) {
                    add("${crunchyrollEpisode.seriesTitle} $romanNumber")
                }

                add("${crunchyrollEpisode.seriesTitle} (${crunchyrollEpisode.seasonDisplayNumber})")
            }
        }
    }

    fun getAniDBAnimeTitles(locale: String): Map<Long, Set<String>> {
        return getAnimeTitles().select("anime")
            .asSequence()
            .associate { it.attr("aid").toLong() to it.select("title[xml:lang=\"${locale.split("-").first()}\"],title[xml:lang=\"x-jat\"]").asSequence().map { it.text() }.toSet() }
            .filter { it.value.isNotEmpty() }
    }

    private fun findMatches(seriesTitle: String, aniDbAnimes: Map<Long, Set<String>>): Map<Map.Entry<Long, Set<String>>, Int> {
        return aniDbAnimes.entries.asSequence()
            .associateWith { it.value.minOf { levenshteinDistance(it, seriesTitle) } }
            .filter { it.value <= 3 }
            .let { cm -> cm.filter { it.value == cm.minByOrNull { it.value }?.value } }
    }

    private fun searchForMatchesInLocales(locales: Set<String>, episodeId: String): Map<Map.Entry<Long, Set<String>>, Int> {
        locales.forEach { locale ->
            val aniDbAnimes = getAniDBAnimeTitles(locale)
            val seriesTitles = runBlocking { getSeriesTitlesWithEpisodeId(locale, episodeId) }
            println("Series titles for $episodeId: $seriesTitles")

            seriesTitles.forEach { seriesTitle ->
                findMatches(seriesTitle, aniDbAnimes).takeIf { it.isNotEmpty() }?.let {
                    println("Matches for $seriesTitle: $it")
                    return it
                }
            }
        }

        return emptyMap()
    }

    private fun searchShortNameInLocales(locales: Set<String>, episodeId: String, addShortNames: Boolean = false): Map<Map.Entry<Long, Set<String>>, Int> {
        locales.forEach { locale ->
            val aniDbAnimes = getAniDBAnimeTitles(locale).toMutableMap()

            if (addShortNames) {
                aniDbAnimes.toMap().forEach { (id, titles) ->
                    val newTitles = titles.toMutableSet()
                    newTitles.addAll(titles.map { StringUtils.getShortName(it) }.toSet())
                    aniDbAnimes[id] = newTitles
                }
            }

            val seriesTitle = StringUtils.getShortName(runBlocking { CrunchyrollCachedWrapper.getEpisode(locale, episodeId).seriesTitle })
            println("Series title for $episodeId: $seriesTitle")
            findMatches(seriesTitle, aniDbAnimes).takeIf { it.isNotEmpty() }?.let { return it }
        }

        return emptyMap()
    }

    fun searchAniDBAnimeTitle(locales: Set<String>, episodeId: String): Map<Map.Entry<Long, Set<String>>, Int> {
        searchForMatchesInLocales(locales, episodeId).takeIf { it.isNotEmpty() }?.let { return it }
        searchShortNameInLocales(locales, episodeId).takeIf { it.isNotEmpty() }?.let { return it }
        searchShortNameInLocales(locales, episodeId, true).takeIf { it.isNotEmpty() }?.let { return it }
        return emptyMap()
    }
}