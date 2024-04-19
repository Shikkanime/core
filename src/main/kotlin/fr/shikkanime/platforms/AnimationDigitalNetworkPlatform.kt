package fr.shikkanime.platforms

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.inject.Inject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.AnimeException
import fr.shikkanime.exceptions.AnimeNotSimulcastedException
import fr.shikkanime.platforms.configuration.AnimationDigitalNetworkConfiguration
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.ObjectParser.getAsBoolean
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsLong
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import java.io.File
import java.time.ZonedDateTime
import java.util.logging.Level

class AnimationDigitalNetworkPlatform :
    AbstractPlatform<AnimationDigitalNetworkConfiguration, CountryCode, List<JsonObject>>() {
    @Inject
    private lateinit var configCacheService: ConfigCacheService

    override fun getPlatform(): Platform = Platform.ANIM

    override fun getConfigurationClass() = AnimationDigitalNetworkConfiguration::class.java

    override suspend fun fetchApiContent(key: CountryCode, zonedDateTime: ZonedDateTime): List<JsonObject> {
        return AnimationDigitalNetworkWrapper.getLatestVideos(zonedDateTime.toLocalDate())
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            val api = parseAPIContent(bypassFileContent, countryCode, "videos", zonedDateTime)

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

    private fun convertEpisode(
        countryCode: CountryCode,
        jsonObject: JsonObject,
        zonedDateTime: ZonedDateTime
    ): List<Episode> {
        val show = requireNotNull(jsonObject.getAsJsonObject("show")) { "Show is null" }
        val season = jsonObject.getAsString("season")?.toIntOrNull() ?: 1

        var animeName =
            requireNotNull(show.getAsString("shortTitle") ?: show.getAsString("title")) { "Anime name is null" }
        animeName = animeName.replace(Regex("Saison \\d"), "").trim()
        animeName = animeName.replace(season.toString(), "").trim()
        animeName = animeName.replace(Regex(" -.*"), "").trim()
        animeName = animeName.replace(Regex(" Part.*"), "").trim()
        if (configuration!!.blacklistedSimulcasts.contains(animeName.lowercase())) throw AnimeException("\"$animeName\" is blacklisted")

        val animeImage = requireNotNull(show.getAsString("image2x")) { "Anime image is null" }
        val animeBanner = requireNotNull(show.getAsString("imageHorizontal2x")) { "Anime banner is null" }
        val animeDescription = show.getAsString("summary")?.replace('\n', ' ') ?: ""
        val genres = show.getAsJsonArray("genres") ?: JsonArray()

        val contains = configuration!!.simulcasts.map { it.name.lowercase() }.contains(animeName.lowercase())
        if ((genres.isEmpty || !genres.any {
                it.asString.startsWith(
                    "Animation ",
                    true
                )
            }) && !contains) throw Exception("Anime is not an animation")

        var isSimulcasted = show.getAsBoolean("simulcast") == true ||
                show.getAsString("firstReleaseYear") in (0..1).map { (zonedDateTime.year - it).toString() } ||
                contains

        val descriptionLowercase = animeDescription.lowercase()

        isSimulcasted = isSimulcasted ||
                configCacheService.getValueAsString(ConfigPropertyKey.ANIMATION_DITIGAL_NETWORK_SIMULCAST_DETECTION_REGEX)
                    ?.let {
                        Regex(it).containsMatchIn(descriptionLowercase)
                    } == true

        if (!isSimulcasted) throw AnimeNotSimulcastedException("Anime is not simulcasted")

        val releaseDateString = requireNotNull(jsonObject.getAsString("releaseDate")) { "Release date is null" }
        val releaseDate = ZonedDateTime.parse(releaseDateString)

        val numberAsString = jsonObject.getAsString("shortNumber")
        val showType = show.getAsString("type")

        if (numberAsString?.startsWith("Bande-annonce") == true ||
            numberAsString?.startsWith("Bande annonce") == true ||
            numberAsString?.startsWith("Court-métrage") == true ||
            showType == "PV"
        ) throw Exception(
            "Anime is a trailer"
        )

        val (number, episodeType) = getNumberAndEpisodeType(numberAsString, showType)

        val id = jsonObject.getAsInt("id")
        val title = jsonObject.getAsString("name")?.ifBlank { null }
        val url = requireNotNull(jsonObject.getAsString("url")) { "Url is null" }
        val image = requireNotNull(jsonObject.getAsString("image2x")) { "Image is null" }
        val duration = jsonObject.getAsLong("duration", -1)
        val description = jsonObject.getAsString("summary")?.replace('\n', ' ')?.ifBlank { null }

        return jsonObject.getAsJsonArray("languages").map {
            Episode(
                countryCode = countryCode,
                anime = animeName,
                animeImage = animeImage,
                animeBanner = animeBanner,
                animeDescription = animeDescription,
                releaseDateTime = releaseDate,
                episodeType = episodeType,
                season = season,
                number = number,
                duration = duration,
                title = title,
                description = description,
                image = image,
                platform = getPlatform(),
                audioLocale = getAudioLocale(it),
                id = id.toString(),
                url = url,
                uncensored = jsonObject.getAsString("title")?.contains("(NC)") == true,
            )
        }
    }

    private fun getNumberAndEpisodeType(numberAsString: String?, showType: String?): Pair<Int, EpisodeType> {
        val number = numberAsString?.replace("\\(.*\\)".toRegex(), "")?.trim()?.toIntOrNull() ?: -1

        var episodeType = when {
            numberAsString == "OAV" || numberAsString == "Épisode spécial" || showType == "OAV" || numberAsString?.contains(
                "."
            ) == true -> EpisodeType.SPECIAL

            numberAsString == "Film" -> EpisodeType.FILM
            else -> EpisodeType.EPISODE
        }

        "Épisode spécial (\\d*)".toRegex().find(numberAsString ?: "")?.let {
            episodeType = EpisodeType.SPECIAL
            it.groupValues[1].toIntOrNull()?.let { specialNumber -> return Pair(specialNumber, episodeType) }
        }

        return Pair(number, episodeType)
    }

    private fun getAudioLocale(it: JsonElement): String {
        return when (it.asString) {
            "vostf" -> "ja-JP"
            "vf" -> "fr-FR"
            else -> throw Exception("Language is null")
        }
    }
}