package fr.shikkanime

import fr.shikkanime.utils.FileManager
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.wrappers.AnilistWrapper
import fr.shikkanime.wrappers.JikanWrapper
import fr.shikkanime.wrappers.factories.AbstractAnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import fr.shikkanime.wrappers.impl.caches.AnimationDigitalNetworkCachedWrapper
import fr.shikkanime.wrappers.impl.caches.CrunchyrollCachedWrapper
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.ZonedDateTime

suspend fun main() {
    // https://www.crunchyroll.com/fr/watch/GZ7UV4ZQ1/gods-blessings-on-these-wonderful-works-of-art
    // GZ7UV4ZQ1
    val locale = "en-US"

    getMalEpisodesWithCrunchyrollEpisode(locale, "GZ7UV4ZQ1")
    println("-".repeat(50))
    // God's Blessing on This Wonderful Party
    // God's Blessing on This Wonderful Party!
    getMalEpisodesWithCrunchyrollEpisode(locale, "GVWU09KNQ")
    println("-".repeat(50))
    getMalEpisodesWithCrunchyrollEpisode(locale, "GN7UDZ0M5")
    println("-".repeat(50))
    getMalEpisodesWithCrunchyrollEpisode(locale, "G4VUQ2D55")
    println("-".repeat(50))
    getMalEpisodesWithCrunchyrollEpisode(locale, "GJWU2VWEW")
    println("-".repeat(50))
    getMalEpisodesWithCrunchyrollEpisode(locale, "GWDU89ZKX")
    println("-".repeat(50))
    getMalEpisodesWithCrunchyrollEpisode(locale, "G7PU45P20")
    println("-".repeat(50))
    getMalEpisodesWithCrunchyrollEpisode(locale, "GVWU0ZJPZ")
    println("-".repeat(50))
    getMalEpisodesWithCrunchyrollEpisode(locale, "GG1U2MW1W")
    println("-".repeat(50))
    getMalEpisodesWithCrunchyrollEpisode(locale, "GJWU2XZQW")
    println("-".repeat(50))
    getMalEpisodesWithCrunchyrollEpisode(locale, "G2XU03JV5")
    println("-".repeat(50))
    getMalEpisodesWithCrunchyrollEpisode(locale, "GG1U2381P")
    println("-".repeat(50))
    getMalEpisodesWithCrunchyrollEpisode(locale, "GZ7UV0Z53")
    println("-".repeat(50))
    getMalEpisodesWithADNEpisode(14352)
    println("-".repeat(50))
    getMalEpisodesWithADNEpisode(14387)
    println("-".repeat(50))
    getMalEpisodesWithADNEpisode(23611)
    println("-".repeat(50))
    getMalEpisodesWithADNEpisode(13792)
    println("-".repeat(50))
    getMalEpisodesWithADNEpisode(2719)
    println("-".repeat(50))
    getMalEpisodesWithADNEpisode(14074)
    println("-".repeat(50))
    getMalEpisodesWithADNEpisode(23189)
}

private fun String.onlyLettersAndSpaces() = replace(Regex("[^A-Za-z ]"), "")

private fun <T> getOrSetDefault(key: String, javaClass: Class<T>, default: suspend () -> T): T {
    val file = File("data", "tmp.shikk")

    val tmpDatabase: MutableMap<String, String> = if (file.exists())
        FileManager.readFile<Map<String, String>>(file).toMutableMap()
    else {
        file.createNewFile()
        mutableMapOf()
    }

    val value = tmpDatabase[key]
    
    return if (value == null) {
        val newValue = runBlocking { default() }
        tmpDatabase[key] = ObjectParser.toJson(newValue)
        FileManager.writeFile(file, tmpDatabase)
        newValue
    } else {
        ObjectParser.fromJson(value, javaClass)
    }
}

private suspend fun getMalEpisodesWithCrunchyrollEpisode(
    locale: String,
    crunchyrollEpisodeId: String
) = runCatching {
    val crunchyrollEpisode = CrunchyrollCachedWrapper.getEpisode(locale, crunchyrollEpisodeId).also { println(it) }

    val searchVariations = listOf(
        createSearchTriple(crunchyrollEpisode, addPart2 = false),
        createSearchTriple(crunchyrollEpisode, addPart2 = true)
    )

    searchVariations.forEach { (series, season, episode) ->
        val anilist = getAnilistMedia(series, season, episode)?.also { println(it) } ?: run {
            println("Anilist not found")
            return@forEach
        }

        val malAnime = getOrSetDefault("malAnime_${anilist.idMal}", JikanWrapper.Anime::class.java) {
            JikanWrapper.getAnime(anilist.idMal!!)
        }.also { println(it) }

        val malEpisodes = getMalEpisodesList(anilist.idMal!!).also { println(it) }

        val malEpisode = findMatchingEpisode(malEpisodes, crunchyrollEpisode, season)?.also { println(it) } ?: run {
            println("MAL episode not found${if (season?.contains("Part 2") == true) "..." else ", searching by adding part 2 on season name..."}")
            return@forEach
        }

        val malAired = malEpisode.aired ?: malEpisodes.takeIf { it.size == 1 }?.let { malAnime.aired?.from }

        println(crunchyrollEpisode.premiumAvailableDate)
        println(malAired)

        val isCorrectReleaseDate = if (malAired == null) {
            validatePlatformReleaseDate(malAnime, crunchyrollEpisode.premiumAvailableDate)
        } else {
            validateMalAiredDate(malAired, crunchyrollEpisode.premiumAvailableDate)
        }

        if (malAired != null || isCorrectReleaseDate) return@runCatching
    }
}

private suspend fun getMalEpisodesWithADNEpisode(adnEpisodeId: Int) = runCatching {
    val adnEpisode = AnimationDigitalNetworkCachedWrapper.getVideo(adnEpisodeId).also { println(it) }

    val searchVariations = listOf(
        createSearchTriple(adnEpisode.show.originalTitle, addPart2 = false),
        createSearchTriple(adnEpisode.show.originalTitle, addPart2 = true),
        createSearchTriple(adnEpisode.show.originalTitle, addPart2 = true, season = true),
        createSearchTriple(adnEpisode.show.shortTitle, addPart2 = false),
        createSearchTriple(adnEpisode.show.shortTitle, addPart2 = true),
        createSearchTriple(adnEpisode.show.shortTitle, addPart2 = true, season = true),
        createSearchTriple(adnEpisode.show.title, addPart2 = false),
        createSearchTriple(adnEpisode.show.title, addPart2 = true),
        createSearchTriple(adnEpisode.show.title, addPart2 = true, season = true),
    ).distinct()

    searchVariations.filterNotNull().forEach { series ->
        val anilist = getAnilistMedia(series, null, null)?.also { println(it) } ?: run {
            println("Anilist not found")
            return@forEach
        }

        val malAnime = getOrSetDefault("malAnime_${anilist.idMal}", JikanWrapper.Anime::class.java) {
            JikanWrapper.getAnime(anilist.idMal!!)
        }.also { println(it) }

        val malEpisodes = getMalEpisodesList(anilist.idMal!!).also { println(it) }

        val malEpisode = findMatchingEpisode(malEpisodes, adnEpisode, series)?.also { println(it) } ?: run {
            println("MAL episode not found${if (series.contains("Part 2") || series.contains("Season 2")) "..." else ", searching by adding part 2 on season name..."}")
            return@forEach
        }

        val malAired = malEpisode.aired ?: malEpisodes.takeIf { it.size == 1 }?.let { malAnime.aired?.from }

        println(adnEpisode.releaseDate)
        println(malAired)

        val isCorrectReleaseDate = if (malAired == null) {
            validatePlatformReleaseDate(malAnime, adnEpisode.releaseDate)
        } else {
            validateMalAiredDate(malAired, adnEpisode.releaseDate)
        }

        if (malAired != null || isCorrectReleaseDate) return@runCatching
    }
}

private fun getMalEpisodesList(idMal: Int) =
    (getOrSetDefault("malAnime_${idMal}_episodes", Array<JikanWrapper.Episode>::class.java) {
        JikanWrapper.getEpisodes(idMal).toTypedArray()
    }.takeIf { it.isNotEmpty() }
        ?: getOrSetDefault("malAnime_${idMal}_videos_episodes", Array<JikanWrapper.Episode>::class.java) {
            JikanWrapper.getVideosEpisodes(idMal).toTypedArray()
        }).toList()

private fun validatePlatformReleaseDate(malAnime: JikanWrapper.Anime, releaseDateTime: ZonedDateTime): Boolean {
    val from = malAnime.aired?.from ?: return false
    val to = malAnime.aired?.to ?: ZonedDateTime.now()

    if (releaseDateTime in from..to) {
        println("Release date is correct!")
        return true
    } else {
        println("Release date is incorrect! ($from - $to)")
        return false
    }
}

private fun validateMalAiredDate(malAired: ZonedDateTime, releaseDateTime: ZonedDateTime): Boolean {
    val from = releaseDateTime.minusDays(2)
    val to = releaseDateTime.plusDays(2)

    if (malAired in from..to) {
        println("Release date is correct!")
        return true
    } else {
        println("Release date is incorrect! (${malAired.minusDays(2)} - ${malAired.plusDays(2)})")
        return false
    }
}

private fun createSearchTriple(episode: AbstractCrunchyrollWrapper.Episode, addPart2: Boolean) = Triple(
    if (addPart2 && episode.seasonTitle.isNullOrBlank()) "${episode.seriesTitle.trim()} Part 2"
    else episode.seriesTitle.trim(),
    if (addPart2 && !episode.seasonTitle.isNullOrBlank()) "${episode.seasonTitle!!.trim()} Part 2"
    else episode.seasonTitle?.trim(),
    episode.title?.trim()
)

private fun createSearchTriple(seriesName: String?, addPart2: Boolean, season: Boolean = false): String? {
    if (seriesName == null) return null
    val partString = " ${if (season) "Season" else "Part"} 2"

    return if (addPart2) "${seriesName.trim()}$partString"
    else seriesName.trim()
}

private fun findMatchingEpisode(
    malEpisodes: List<JikanWrapper.Episode>,
    crunchyrollEpisode: AbstractCrunchyrollWrapper.Episode,
    season: String?
) = malEpisodes.firstOrNull {
    it.title.onlyLettersAndSpaces().lowercase() == crunchyrollEpisode.title?.onlyLettersAndSpaces()?.lowercase() ||
            (season?.contains("Part 2") == true && it.malId == crunchyrollEpisode.number!! - malEpisodes.size) ||
            it.malId == crunchyrollEpisode.number!!
}

private fun getNumber(numberAsString: String?): Int {
    val number = numberAsString?.replace("\\(.*\\)".toRegex(), "")?.trim()?.toIntOrNull() ?: 1
    val specialMatch = "Épisode spécial (\\d*)".toRegex().find(numberAsString ?: "")

    if (specialMatch != null) {
        val specialNumber = specialMatch.groupValues[1].toIntOrNull()
        return (specialNumber ?: number)
    }

    return number
}

private fun findMatchingEpisode(
    malEpisodes: List<JikanWrapper.Episode>,
    adnEpisode: AbstractAnimationDigitalNetworkWrapper.Video,
    season: String?
): JikanWrapper.Episode? {
    val number = getNumber(adnEpisode.shortNumber)

    return malEpisodes.firstOrNull {
        it.title.onlyLettersAndSpaces().lowercase() == adnEpisode.title.onlyLettersAndSpaces().lowercase() ||
                ((season?.contains("Part 2") == true || season?.contains("Season 2") == true) && it.malId == number - malEpisodes.size) ||
                it.malId == number
    }
}

private fun getAnilistMedia(
    seriesTitle: String,
    seasonTitle: String?,
    episodeTitle: String?
): AnilistWrapper.Media? {
    val searchAttempts = listOf(
        "$seriesTitle $episodeTitle".takeIf { !episodeTitle.isNullOrBlank() },
        seasonTitle ?: seriesTitle,
        seriesTitle,
    )

    return searchAttempts
        .filterNotNull()
        .map { it.replace("(French / German Europe)", "").trim() }
        .firstNotNullOfOrNull { search ->
            runCatching {
                getOrSetDefault(search, AnilistWrapper.Media::class.java) {
                    AnilistWrapper.getMedia(search)
                }
            }.onFailure { exception -> println(exception) }.getOrNull()
        }
}