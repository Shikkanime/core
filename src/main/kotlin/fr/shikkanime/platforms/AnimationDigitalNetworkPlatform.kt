package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.AnimeException
import fr.shikkanime.exceptions.AnimeNotSimulcastedException
import fr.shikkanime.platforms.configuration.AnimationDigitalNetworkConfiguration
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import java.io.File
import java.time.ZonedDateTime
import java.util.logging.Level

class AnimationDigitalNetworkPlatform :
    AbstractPlatform<AnimationDigitalNetworkConfiguration, CountryCode, Array<AnimationDigitalNetworkWrapper.Video>>() {
    @Inject
    private lateinit var configCacheService: ConfigCacheService

    override fun getPlatform(): Platform = Platform.ANIM

    override fun getConfigurationClass() = AnimationDigitalNetworkConfiguration::class.java

    override suspend fun fetchApiContent(key: CountryCode, zonedDateTime: ZonedDateTime): Array<AnimationDigitalNetworkWrapper.Video> {
        return AnimationDigitalNetworkWrapper.getLatestVideos(zonedDateTime.toLocalDate())
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            val api = if (bypassFileContent != null && bypassFileContent.exists()) {
                ObjectParser.fromJson(
                    ObjectParser.fromJson(bypassFileContent.readText()).getAsJsonArray("videos"),
                    Array<AnimationDigitalNetworkWrapper.Video>::class.java
                )
            } else {
                getApiContent(countryCode, zonedDateTime) // NOSONAR
            }

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
        video: AnimationDigitalNetworkWrapper.Video,
        zonedDateTime: ZonedDateTime,
        needSimulcast: Boolean = true
    ): List<Episode> {
        val season = video.season?.toIntOrNull() ?: 1

        var animeName = requireNotNull(video.show.shortTitle ?: video.show.title) { "Anime name is null" }
        animeName = animeName.replace(Regex("Saison \\d"), "").trim()
        animeName = animeName.replace(season.toString(), "").trim()
        animeName = animeName.replace(Regex(" -.*"), "").trim()
        animeName = animeName.replace(Regex(" Part.*"), "").trim()
        if (configuration!!.blacklistedSimulcasts.contains(animeName.lowercase())) throw AnimeException("\"$animeName\" is blacklisted")

        val animeImage = requireNotNull(video.show.image2x) { "Anime image is null" }
        val animeBanner = requireNotNull(video.show.imageHorizontal2x) { "Anime banner is null" }
        val animeDescription = video.show.summary?.replace('\n', ' ') ?: ""
        val genres = video.show.genres

        val contains = configuration!!.simulcasts.map { it.name.lowercase() }.contains(animeName.lowercase())
        if ((genres.isEmpty() || !genres.any { it.startsWith("Animation ", true) }) && !contains)
            throw Exception("Anime is not an animation")

        if (needSimulcast) {
            var isSimulcasted = video.show.simulcast ||
                    video.show.firstReleaseYear in (0..1).map { (zonedDateTime.year - it).toString() } ||
                    contains

            val descriptionLowercase = animeDescription.lowercase()

            isSimulcasted = isSimulcasted ||
                    configCacheService.getValueAsString(ConfigPropertyKey.ANIMATION_DITIGAL_NETWORK_SIMULCAST_DETECTION_REGEX)
                        ?.let {
                            Regex(it).containsMatchIn(descriptionLowercase)
                        } == true

            if (!isSimulcasted) throw AnimeNotSimulcastedException("Anime is not simulcasted")
        }

        val trailerIndicators = listOf("Bande-annonce", "Bande annonce", "Court-métrage", "Opening", "Making-of")
        val specialShowTypes = listOf("PV", "BONUS")

        if (trailerIndicators.any { video.shortNumber?.startsWith(it) == true } || specialShowTypes.contains(video.type)) {
            throw Exception("Anime is not an episode")
        }

        val (number, episodeType) = getNumberAndEpisodeType(video.shortNumber, video.type)

        val url = requireNotNull(video.url) { "Url is null" }
        val image = requireNotNull(video.image2x) { "Image is null" }
        val description = video.summary?.replace('\n', ' ')?.ifBlank { null }

        return video.languages.map {
            Episode(
                countryCode = countryCode,
                anime = animeName,
                animeImage = animeImage,
                animeBanner = animeBanner,
                animeDescription = animeDescription,
                releaseDateTime = video.releaseDate,
                episodeType = episodeType,
                season = season,
                number = number,
                duration = video.duration,
                title = video.name?.ifBlank { null },
                description = description,
                image = image,
                platform = getPlatform(),
                audioLocale = getAudioLocale(it),
                id = video.id.toString(),
                url = url,
                uncensored = video.title.contains("(NC)"),
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

    private fun getAudioLocale(string: String): String {
        return when (string) {
            "vostf" -> "ja-JP"
            "vf" -> "fr-FR"
            else -> throw Exception("Language is null")
        }
    }
}