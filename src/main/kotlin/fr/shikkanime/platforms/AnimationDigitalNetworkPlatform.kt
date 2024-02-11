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
import fr.shikkanime.utils.ObjectParser.getAsBoolean
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsLong
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import java.io.File
import java.time.ZonedDateTime
import java.util.logging.Level

class AnimationDigitalNetworkPlatform :
    AbstractPlatform<AnimationDigitalNetworkConfiguration, CountryCode, List<JsonObject>>() {
    override fun getPlatform(): Platform = Platform.ANIM

    override suspend fun fetchApiContent(key: CountryCode, zonedDateTime: ZonedDateTime): List<JsonObject> {
        return AnimationDigitalNetworkWrapper.getLatestVideos(zonedDateTime.toLocalDate())
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            val api = getApiContent(countryCode, zonedDateTime)

            api.forEach {
                try {
                    list.addAll(convertEpisode(countryCode, it, zonedDateTime))
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
    ): List<Episode> {
        val show = requireNotNull(jsonObject.getAsJsonObject("show")) { "Show is null" }
        val season = jsonObject.getAsString("season")?.toIntOrNull() ?: 1

        var animeName = requireNotNull(show.getAsString("shortTitle") ?: show.getAsString("title")) { "Anime name is null" }
        animeName = animeName.replace(Regex("Saison \\d"), "").trim()
        animeName = animeName.replace(season.toString(), "").trim()
        animeName = animeName.replace(Regex(" -.*"), "").trim()
        animeName = animeName.replace(Regex(" Part.*"), "").trim()

        val animeImage = requireNotNull(show.getAsString("image2x")) { "Anime image is null" }
        val animeBanner = requireNotNull(show.getAsString("imageHorizontal2x")) { "Anime banner is null" }
        val animeDescription = show.getAsString("summary")?.replace('\n', ' ') ?: ""
        val genres = show.getAsJsonArray("genres") ?: JsonArray()

        val contains = configuration!!.simulcasts.map { it.name.lowercase() }.contains(animeName.lowercase())
        if ((genres.isEmpty || !genres.any { it.asString.startsWith("Animation ", true) }) && !contains) throw Exception("Anime is not an animation")

        var isSimulcasted = show.getAsBoolean("simulcast") == true ||
                show.getAsString("firstReleaseYear") in (0..1).map { (zonedDateTime.year - it).toString() } ||
                contains

        val descriptionLowercase = animeDescription.lowercase()

        isSimulcasted = isSimulcasted || descriptionLowercase.startsWith("(premier épisode ") ||
                descriptionLowercase.startsWith("(diffusion des ") ||
                descriptionLowercase.startsWith("(diffusion du premier épisode") ||
                descriptionLowercase.startsWith("(diffusion de l'épisode 1 le")

        if (!isSimulcasted) throw AnimeNotSimulcastedException("Anime is not simulcasted")

        val releaseDateString = requireNotNull(jsonObject.getAsString("releaseDate")) { "Release date is null" }
        val releaseDate = ZonedDateTime.parse(releaseDateString)

        val numberAsString = jsonObject.getAsString("shortNumber")
        if (numberAsString?.startsWith("Bande-annonce") == true || numberAsString?.startsWith("Bande annonce") == true || numberAsString?.startsWith("Court-métrage") == true) throw Exception(
            "Anime is a trailer"
        )

        val number = numberAsString?.replace("\\(.*\\)".toRegex(), "")?.trim()?.toIntOrNull() ?: -1

        var episodeType = when (numberAsString) {
            "OAV", "Épisode spécial" -> EpisodeType.SPECIAL
            "Film" -> EpisodeType.FILM
            else -> EpisodeType.EPISODE
        }

        if (numberAsString?.contains(".") == true || show.getAsString("type") == "OAV") episodeType = EpisodeType.SPECIAL

        val id = jsonObject.getAsInt("id")
        val title = jsonObject.getAsString("name")?.ifBlank { null }
        val url = requireNotNull(jsonObject.getAsString("url")) { "Url is null" }
        val image = requireNotNull(jsonObject.getAsString("image2x")) { "Image is null" }
        val duration = jsonObject.getAsLong("duration", -1)
        val description = jsonObject.getAsString("summary")?.replace('\n', ' ')?.ifBlank { null }

        return jsonObject.getAsJsonArray("languages").map {
            val langType = when (it.asString) {
                "vostf" -> LangType.SUBTITLES
                "vf" -> LangType.VOICE
                else -> throw Exception("Language is null")
            }

            Episode(
                platform = getPlatform(),
                anime = Anime(
                    countryCode = countryCode,
                    name = animeName,
                    releaseDateTime = releaseDate,
                    image = animeImage,
                    banner = animeBanner,
                    description = animeDescription,
                    slug = StringUtils.toSlug(StringUtils.getShortName(animeName)),
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
                description = description
            )
        }
    }
}