package fr.shikkanime.platforms

import com.google.gson.JsonObject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Country
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.Platform
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.ZonedDateTime

class AnimationDigitalNetworkPlatform : AbstractPlatform<PlatformConfiguration>() {
    override fun getConfigurationClass(): Class<PlatformConfiguration> = PlatformConfiguration::class.java

    override fun getPlatform(): Platform {
        return Platform(
            name = "Animation Digital Network",
            url = "https://animationdigitalnetwork.fr/",
            image = "animation_digital_network.png",
        )
    }

    override suspend fun fetchApiContent(zonedDateTime: ZonedDateTime): Api {
        val map = mutableMapOf<String, String>()
        val countries = getCountries()
        val toDateString = zonedDateTime.toLocalDate().toString()

        countries.forEach { country ->
            val url = "https://gw.api.animationdigitalnetwork.${country.countryCode!!.lowercase()}/video/calendar?date=$toDateString"
            val response = HttpRequest().get(url)

            if (response.status != HttpStatusCode.OK) {
                return@forEach
            }

            map[country.countryCode] = response.bodyAsText()
        }

        return Api(zonedDateTime, map)
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime): List<Episode> {
        val list = mutableListOf<Episode>()
        val countries = getCountries()

        countries.forEach { country ->
            val api = getApiContent(country, zonedDateTime)
            val array = Constant.gson.fromJson(api, JsonObject::class.java).getAsJsonArray("videos")

            array.forEach {
                try {
                    list.add(convertEpisode(country, it.asJsonObject, zonedDateTime))
                } catch (e: Exception) {
                    // Ignore
                    e.printStackTrace()
                }
            }
        }

        return list
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    private fun convertEpisode(country: Country, jsonObject: JsonObject, zonedDateTime: ZonedDateTime): Episode {
        val show = jsonObject.getAsJsonObject("show") ?: throw Exception("Show is null")

        var animeName = show["shortTitle"]?.asString ?: show["title"]?.asString ?: throw Exception("Anime name is null")
        animeName = animeName.replace(Regex("Saison \\d"), "").trim('-').trim()

        val animeImage = show["image2x"]?.asString ?: throw Exception("Anime image is null")
        val animeDescription = show["summary"]?.asString ?: ""
        val genres = show.getAsJsonArray("genres")?.toList() ?: emptyList()

        if (genres.isEmpty() || !genres.any { it.asString.startsWith("Animation ", true) }) {
            throw Exception("Anime is not an animation")
        }

        var isSimulcasted = show["simulcast"]?.asBoolean == true ||
                show["firstReleaseYear"]?.asString == zonedDateTime.toLocalDate().year.toString() ||
                configuration?.simulcasts?.contains(animeName) == true

        val descriptionLowercase = animeDescription.lowercase()

        isSimulcasted = isSimulcasted || descriptionLowercase.startsWith("(premier épisode ") ||
                descriptionLowercase.startsWith("(diffusion des ") ||
                descriptionLowercase.startsWith("(diffusion du premier épisode") ||
                descriptionLowercase.startsWith("(diffusion de l'épisode 1 le")

        if (!isSimulcasted) {
            throw Exception("Anime is not simulcasted")
        }

        val releaseDateString = jsonObject["releaseDate"]?.asString ?: throw Exception("Release date is null")
        // Example : 2023-12-08T13:30:00Z
        val releaseDate = ZonedDateTime.parse(releaseDateString)

        val season = show["season"]?.asString?.toIntOrNull() ?: 1

        val numberAsString = jsonObject["shortNumber"]?.asString

        if (numberAsString?.startsWith("Bande-annonce") == true) {
            throw Exception("Anime is a trailer")
        }

        val number = numberAsString?.toIntOrNull() ?: -1

        val episodeType = when (numberAsString) {
            "OAV" -> EpisodeType.SPECIAL
            "Film" -> EpisodeType.FILM
            else -> EpisodeType.EPISODE
        }

        val langType = when (jsonObject.getAsJsonArray("languages").lastOrNull()?.asString) {
            "vostf" -> LangType.SUBTITLES
            "vf" -> LangType.VOICE
            else -> throw Exception("Language is null")
        }

        val id = jsonObject["id"]?.asLong ?: throw Exception("Id is null")

        val title = jsonObject["name"]?.asString?.ifBlank { null }

        val url = jsonObject["url"]?.asString ?: throw Exception("Url is null")

        val image = jsonObject["image2x"]?.asString ?: throw Exception("Image is null")

        val duration = jsonObject["duration"]?.asLong ?: -1

        return Episode(
            platform = getPlatform(),
            anime = Anime(
                country = country,
                name = animeName,
                releaseDate = releaseDate,
                image = animeImage,
                description = animeDescription,
            ),
            episodeType = episodeType,
            langType = langType,
            hash = id.toString(),
            releaseDate = releaseDate,
            season = season,
            number = number,
            title = title,
            url = url,
            image = image,
            duration = duration,
        )
    }
}