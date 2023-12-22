package fr.shikkanime

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.AnimeException
import fr.shikkanime.platforms.AnimationDigitalNetworkPlatform
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsLong
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.ObjectParser.getNullableJsonObject
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

private const val DD_MM_YYYY = "dd/MM/yyyy"
private val ofPattern = DateTimeFormatter.ofPattern(DD_MM_YYYY)

// Session id is a cookie when you are logged in on https://beta-api.crunchyroll.com/
private const val SESSION_ID = "0095dc7baeb182a071fe7b7b1af7a5e3"

private val animes = MapCache<String, AnimeDto> {
    runBlocking {
        // Session id is a cookie when you are logged in on https://beta-api.crunchyroll.com/
        val response =
            HttpRequest().get("https://api.crunchyroll.com/info.0.json?session_id=$SESSION_ID&series_id=$it&locale=fr")
        val json = ObjectParser.fromJson(response.bodyAsText(), JsonObject::class.java).getAsJsonObject("data")
        val name = json.getAsString("name")!!
        val image = json.getAsJsonObject("portrait_image").getAsString("full_url")
        val description = json.getAsString("description")?.replace("[\\n\\r]".toRegex(), "")

        AnimeDto(
            null,
            CountryCode.FR,
            name,
            "",
            image,
            description,
            simulcasts = emptyList(),
        )
    }
}

private val collections = MapCache<String, Pair<String, Long>> {
    runBlocking {
        // Session id is a cookie when you are logged in on https://beta-api.crunchyroll.com/
        val response =
            HttpRequest().get("https://api.crunchyroll.com/info.0.json?session_id=$SESSION_ID&collection_id=$it&locale=fr")
        val json = ObjectParser.fromJson(response.bodyAsText(), JsonObject::class.java).getAsJsonObject("data")
        var season = json.getAsLong("season", 1)

        if (season <= 0L) {
            season = 1
        }

        json.getAsString("name")!! to season
    }
}

fun main() {
    val now = ZonedDateTime.now()
    val httpRequest = HttpRequest()

    runBlocking {
        httpRequest.get("https://www.crunchyroll.com/fr/rss/anime?lang=frFR")
    }

    println("Enter the end date you want to check (${DD_MM_YYYY}):")
    val checkDate = readlnOrNull() ?: return
    val start = System.currentTimeMillis()

    val localDate = try {
        when (checkDate) {
            "-1" -> now.minusDays(1).minusMonths(3).toLocalDate()
            else -> LocalDate.parse(checkDate, ofPattern)
        }
    } catch (e: Exception) {
        println("Invalid date (${e.message})")
        return
    }

    val checkDays = localDate.until(now, ChronoUnit.DAYS).takeIf { it >= 1 } ?: run {
        println("Invalid number of days (must be >= 1)")
        return
    }

    val weeks = (1..checkDays).map { now.minusDays(it).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
        .distinct().sortedByDescending { it }

    val dates = (1..checkDays).map { now.minusDays(it) }.distinct().sortedByDescending { it }

    println("Checking animes of the ${weeks.size} last weeks:")
    weeks.forEach { println("  - ${it.format(ofPattern)}") }
    println()

    val episodes = mutableSetOf<EpisodeDto>()

    dates.parallelStream().forEach { date ->
        val adnPlatform = Constant.injector.getInstance(AnimationDigitalNetworkPlatform::class.java)
        val jsonEpisodes = runBlocking { adnPlatform.fetchApiContent(CountryCode.FR, date) }

        jsonEpisodes.mapNotNull { episodeJson ->
            try {
                AbstractConverter.convert(
                    adnPlatform.convertEpisode(
                        CountryCode.FR,
                        episodeJson.getAsJsonObject(),
                        date
                    ), EpisodeDto::class.java
                )
            } catch (_: AnimeException) {
                // Ignore
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }.also { episodes.addAll(it) }
    }

    weeks.forEachIndexed { _, zonedDateTime ->
        val url = "https://www.crunchyroll.com/fr/simulcastcalendar?filter=premium&date=${
            zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }"
        val html = httpRequest.getBrowser(url, "#template_body > main > section > div > div > ol")

        html.select(".release").parallelStream().forEach { article ->
            val datetime = article.select(".available-time").attr("datetime")
            val time = ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_ZONED_DATE_TIME)

            if (dates.none { date ->
                    date.format(DateTimeFormatter.ISO_LOCAL_DATE) == time.format(DateTimeFormatter.ISO_LOCAL_DATE) && time.isBefore(
                        now
                    )
                }) {
                return@forEach
            }

            val episodeUrl = article.select(".episode-info").attr("href")
            val id = episodeUrl.split("-").last()
            val json = runBlocking { getCrunchyrollEpisode(id, httpRequest, episodeUrl) } ?: return@forEach
            val collectionId = json.getAsString("collection_id")!!
            val collection = collections[collectionId]

            val episodesJson = if (article.attr("data-episode-num").contains("-")) {
                runBlocking {
                    val response =
                        httpRequest.get("https://api.crunchyroll.com/list_media.0.json?session_id=$SESSION_ID&collection_id=$collectionId&limit=50")
                    ObjectParser.fromJson(response.bodyAsText(), JsonObject::class.java).getAsJsonArray("data")
                }
            } else {
                JsonArray().apply { add(json) }
            }

            episodesJson.forEach { episodeJson ->
                getEpisodeDto(episodeJson.asJsonObject, collection)?.let { episodes.add(it) }
            }
        }
    }

    httpRequest.closeBrowser()

    val groupedEpisodes = episodes.groupBy { it.anime.name }

    groupedEpisodes.forEach { (name, episodes) ->
        println("Found ${episodes.size} episodes for $name")

        val anime = episodes.first().anime.copy().apply {
            releaseDateTime = episodes.minOf { it.releaseDateTime }
        }

        episodes.forEach { it.anime = anime }
    }

    println()
    episodes.forEach { episode ->
        println("$episode")
        Constant.injector.getInstance(EpisodeService::class.java)
            .save(AbstractConverter.convert(episode, Episode::class.java))
    }
    println()
    println("Found ${episodes.size} episodes in ${(System.currentTimeMillis() - start) / 1000} seconds")
}

private suspend fun getCrunchyrollEpisode(id: String, httpRequest: HttpRequest, episodeUrl: String): JsonObject? {
    val response = httpRequest.get("https://api.crunchyroll.com/info.0.json?session_id=$SESSION_ID&media_id=$id")

    if (response.status.value != 200) {
        println("Error while getting episode $id: ${response.status}")
        return null
    }

    val bodyAsText = response.bodyAsText()
    val asJsonObject = ObjectParser.fromJson(bodyAsText, JsonObject::class.java).getAsJsonObject("data")

    if (asJsonObject == null || asJsonObject.isJsonNull) {
        println("Error while getting episode $id ($episodeUrl): $bodyAsText")
        return null
    }

    return asJsonObject
}

private fun getEpisodeDto(
    json: JsonObject,
    collection: Pair<String, Long>,
): EpisodeDto? {
    val anime = collection.first
    val isDub = "\\(.* Dub\\)".toRegex().matches(anime)

    if (isDub && !anime.contains("French dub", true)) {
        return null
    }

    val langType = if (anime.contains("(VF)", true) || anime.contains("French dub", true))
        LangType.VOICE
    else
        LangType.SUBTITLES

    val animeId = json.getAsString("series_id")!!
    val animeDto = animes[animeId].copy()
    val isFilm = animeDto.name.contains("Film", true)

    if (isFilm) {
        animeDto.name = animeDto.name.replace("(\\(Film.\\))".toRegex(), "").trim()
    }

    val number = json.getAsInt("episode_number", -1)

    return EpisodeDto(
        null,
        Platform.CRUN,
        animeDto,
        if (isFilm)
            EpisodeType.FILM
        else if (number <= 0)
            EpisodeType.SPECIAL
        else
            EpisodeType.EPISODE,
        langType,
        "${CountryCode.FR}-${Platform.CRUN}-${json.getAsString("media_id")}-$langType",
        json.getAsString("available_time")!!,
        collection.second.toInt(),
        number,
        json.getAsString("name"),
        json.getAsString("url")!!,
        json.getNullableJsonObject("screenshot_image")?.getAsString("full_url") ?: "",
        -1,
    )
}