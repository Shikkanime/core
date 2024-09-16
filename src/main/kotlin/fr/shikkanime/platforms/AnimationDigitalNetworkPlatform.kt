package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.AnimeException
import fr.shikkanime.exceptions.AnimeNotSimulcastedException
import fr.shikkanime.exceptions.NotSimulcastedMediaException
import fr.shikkanime.platforms.configuration.AnimationDigitalNetworkConfiguration
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.normalize
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
                } catch (_: NotSimulcastedMediaException) {
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
        needSimulcast: Boolean = true,
        checkAnimation: Boolean = true
    ): List<Episode> {
        val season = video.season?.toIntOrNull() ?: 1

        val animeName = (video.show.shortTitle ?: video.show.title)
            .replace("(?: -)? Saison \\d|Part.*|$season$".toRegex(), "")
            .trim()

        if (configuration!!.blacklistedSimulcasts.contains(animeName.lowercase())) throw AnimeException("\"$animeName\" is blacklisted")

        val genres = video.show.genres

        val contains = configuration!!.simulcasts.map { it.name.lowercase() }.contains(animeName.lowercase())
        if ((genres.isEmpty() || !genres.any { it.startsWith("Animation ", true) }) && !contains && checkAnimation)
            throw Exception("Anime is not an animation")

        if (needSimulcast) {
            val isSimulcasted = video.show.simulcast ||
                    video.show.firstReleaseYear in (0..1).map { (zonedDateTime.year - it).toString() } ||
                    contains ||
                    configCacheService.getValueAsString(ConfigPropertyKey.ANIMATION_DITIGAL_NETWORK_SIMULCAST_DETECTION_REGEX)
                        ?.let { Regex(it).containsMatchIn((video.show.summary.normalize() ?: "").lowercase()) } == true

            if (!isSimulcasted) throw AnimeNotSimulcastedException("Anime is not simulcasted")
        }

        val trailerIndicators = listOf("Bande-annonce", "Bande annonce", "Court-métrage", "Opening", "Making-of")
        val specialShowTypes = listOf("PV", "BONUS")

        if (trailerIndicators.any { video.shortNumber?.startsWith(it) == true } || specialShowTypes.contains(video.type))
            throw NotSimulcastedMediaException("Trailer or special show type")

        val (number, episodeType) = getNumberAndEpisodeType(video.shortNumber, video.type)

        return video.languages.map {
            Episode(
                countryCode = countryCode,
                animeId = video.show.id.toString(),
                anime = animeName,
                animeImage = video.show.image2x,
                animeBanner = video.show.imageHorizontal2x,
                animeDescription = video.show.summary.normalize(),
                releaseDateTime = video.releaseDate,
                episodeType = episodeType,
                season = season,
                number = number,
                duration = video.duration,
                title = video.name.normalize(),
                description = video.summary.normalize(),
                image = video.image2x,
                platform = getPlatform(),
                audioLocale = getAudioLocale(it),
                id = video.id.toString(),
                url = video.url,
                uncensored = video.title.contains("(NC)", true) || video.title.contains("Non censuré", true),
                original = video.languages.size == 1 || video.languages.indexOf(it) == 0
            )
        }
    }

    private fun getNumberAndEpisodeType(numberAsString: String?, showType: String?): Pair<Int, EpisodeType> {
        val number = numberAsString?.replace("\\(.*\\)".toRegex(), "")?.trim()?.toIntOrNull() ?: -1

        val episodeType = when {
            numberAsString == "Film" -> EpisodeType.FILM
            numberAsString == "OAV" || numberAsString == "Épisode spécial" ||
                    showType == "OAV" || numberAsString?.contains(".") == true -> EpisodeType.SPECIAL
            else -> EpisodeType.EPISODE
        }

        val specialMatch = "Épisode spécial (\\d*)".toRegex().find(numberAsString ?: "")

        if (specialMatch != null) {
            val specialNumber = specialMatch.groupValues[1].toIntOrNull()
            return (specialNumber ?: number) to EpisodeType.SPECIAL
        }

        return number to episodeType
    }

    private fun getAudioLocale(string: String): String {
        return when (string) {
            "vostf" -> "ja-JP"
            "vf" -> "fr-FR"
            else -> throw Exception("Language is null")
        }
    }
}