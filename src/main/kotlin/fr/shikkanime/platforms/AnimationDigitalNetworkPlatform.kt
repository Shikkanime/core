package fr.shikkanime.platforms

import com.google.gson.JsonObject
import fr.shikkanime.entities.Platform
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import io.ktor.client.statement.*
import io.ktor.http.*
import java.time.ZonedDateTime

class AnimationDigitalNetworkPlatform : AbstractPlatform<PlatformConfiguration>() {
    override fun getConfigurationClass(): Class<PlatformConfiguration> = PlatformConfiguration::class.java

    override fun getPlatform(): Platform {
        val name = "Animation Digital Network"

        return platformService.findByName(name) ?: Platform(
            name = name,
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

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime): List<String> {
        val countries = getCountries()

        countries.forEach { country ->
            val api = getApiContent(country, zonedDateTime)
            val array = Constant.gson.fromJson(api, JsonObject::class.java).getAsJsonArray("videos")

            array.forEach {
                convertEpisode(it.asJsonObject, zonedDateTime)
            }
        }

        return emptyList()
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    private fun convertEpisode(jsonObject: JsonObject, zonedDateTime: ZonedDateTime) {
        val show = jsonObject.getAsJsonObject("show") ?: return

        var animeName = show["shortTitle"]?.asString ?: show["title"]?.asString ?: return
        animeName = animeName.replace(Regex("Saison \\d"), "").trim('-').trim()

        val animeImage = show["image2x"]?.asString ?: return
        val animeDescription = show["summary"]?.asString ?: ""
        val genres = show.getAsJsonArray("genres")?.toList() ?: emptyList()

        if (genres.isEmpty() || !genres.any { it.asString.startsWith("Animation ", true) }) {
            return
        }

        var isSimulcasted = show["simulcast"]?.asBoolean == true ||
                show["firstReleaseYear"]?.asString == zonedDateTime.toLocalDate().year.toString()

        val descriptionLowercase = animeDescription.lowercase()

        isSimulcasted = isSimulcasted || descriptionLowercase.startsWith("(premier épisode ") ||
                descriptionLowercase.startsWith("(diffusion des ") ||
                descriptionLowercase.startsWith("(diffusion du premier épisode") ||
                descriptionLowercase.startsWith("(diffusion de l'épisode 1 le")

        println("Anime: $animeName")
        println("Image: $animeImage")
        println("Description: $animeDescription")
        println("Genres: ${genres.joinToString(", ")}")
        println("Simulcasted: $isSimulcasted")
    }
}