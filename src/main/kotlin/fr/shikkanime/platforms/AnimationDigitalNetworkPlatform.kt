package fr.shikkanime.platforms

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.AnimeException
import fr.shikkanime.exceptions.AnimeNotSimulcastedException
import fr.shikkanime.platforms.configuration.AnimationDigitalNetworkConfiguration
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsBoolean
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsLong
import fr.shikkanime.utils.ObjectParser.getAsString
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File
import java.time.ZonedDateTime
import java.util.logging.Level

class AnimationDigitalNetworkPlatform :
    AbstractPlatform<AnimationDigitalNetworkConfiguration, CountryCode, JsonArray>() {
    override fun getPlatform(): Platform = Platform.ANIM

    override suspend fun fetchApiContent(key: CountryCode, zonedDateTime: ZonedDateTime): JsonArray {
        val toDateString = zonedDateTime.toLocalDate().toString()
        val url = "https://gw.api.animationdigitalnetwork.${key.name.lowercase()}/video/calendar?date=$toDateString"
        val response = HttpRequest().get(url)

        if (response.status != HttpStatusCode.OK) {
            return JsonArray()
        }

        return ObjectParser.fromJson(response.bodyAsText(), JsonObject::class.java).getAsJsonArray("videos")!!
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            val api = getApiContent(countryCode, zonedDateTime)

            api.forEach {
                try {
                    list.add(convertEpisode(countryCode, it.asJsonObject, zonedDateTime))
                } catch (_: AnimeException) {
                    // Ignore
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error on converting episode", e)
                }
            }
        }

        return list
    }

    fun convertEpisode(
        countryCode: CountryCode,
        jsonObject: JsonObject,
        zonedDateTime: ZonedDateTime
    ): Episode {
        val show = jsonObject.getAsJsonObject("show") ?: throw Exception("Show is null")

        var animeName =
            show.getAsString("shortTitle") ?: show.getAsString("title") ?: throw Exception("Anime name is null")
        animeName = animeName.replace(Regex("Saison \\d"), "").trim()
        // Replace "Edens Zero -" to get "Edens Zero"
        animeName = animeName.replace(Regex(" -.*"), "").trim()

        val animeImage = show.getAsString("image2x") ?: throw Exception("Anime image is null")
        val animeDescription = show.getAsString("summary")?.replace('\n', ' ') ?: ""
        val genres = show.getAsJsonArray("genres") ?: JsonArray()

        val contains = configuration!!.simulcasts.map { it.name.lowercase() }.contains(animeName.lowercase())

        if ((genres.isEmpty || !genres.any { it.asString.startsWith("Animation ", true) }) && !contains) {
            throw Exception("Anime is not an animation")
        }

        var isSimulcasted = show.getAsBoolean("simulcast") == true ||
                show.getAsString("firstReleaseYear") == zonedDateTime.toLocalDate().year.toString() ||
                contains

        val descriptionLowercase = animeDescription.lowercase()

        isSimulcasted = isSimulcasted || descriptionLowercase.startsWith("(premier épisode ") ||
                descriptionLowercase.startsWith("(diffusion des ") ||
                descriptionLowercase.startsWith("(diffusion du premier épisode") ||
                descriptionLowercase.startsWith("(diffusion de l'épisode 1 le")

        if (!isSimulcasted) {
            throw AnimeNotSimulcastedException("Anime is not simulcasted")
        }

        val releaseDateString = jsonObject.getAsString("releaseDate") ?: throw Exception("Release date is null")
        val releaseDate = ZonedDateTime.parse(releaseDateString)

        val season = jsonObject.getAsString("season")?.toIntOrNull() ?: 1

        val numberAsString = jsonObject.getAsString("shortNumber")

        if (numberAsString?.startsWith("Bande-annonce") == true) {
            throw Exception("Anime is a trailer")
        }

        val number = numberAsString?.toIntOrNull() ?: -1

        var episodeType = when (numberAsString) {
            "OAV" -> EpisodeType.SPECIAL
            "Film" -> EpisodeType.FILM
            else -> EpisodeType.EPISODE
        }

        if (numberAsString?.contains(".") == true) {
            episodeType = EpisodeType.SPECIAL
        }

        val langType = when (jsonObject.getAsJsonArray("languages")?.lastOrNull()?.asString) {
            "vostf" -> LangType.SUBTITLES
            "vf" -> LangType.VOICE
            else -> throw Exception("Language is null")
        }

        val id = jsonObject.getAsInt("id")

        val title = jsonObject.getAsString("name")?.ifBlank { null }

        val url = jsonObject.getAsString("url") ?: throw Exception("Url is null")

        val image = jsonObject.getAsString("image2x") ?: throw Exception("Image is null")

        val duration = jsonObject.getAsLong("duration", -1)

        return Episode(
            platform = getPlatform(),
            anime = Anime(
                countryCode = countryCode,
                name = animeName,
                releaseDateTime = releaseDate,
                image = animeImage,
                description = animeDescription,
            ),
            episodeType = episodeType,
            langType = langType,
            hash = "${countryCode}-${getPlatform()}-$id-$langType",
            releaseDateTime = releaseDate,
            season = season,
            number = number,
            title = title,
            url = url,
            image = image,
            duration = duration,
        )
    }
}