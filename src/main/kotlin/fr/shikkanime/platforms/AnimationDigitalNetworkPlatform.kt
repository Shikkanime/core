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
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.normalize
import fr.shikkanime.wrappers.factories.AbstractAnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.impl.AnimationDigitalNetworkWrapper
import java.io.File
import java.time.ZonedDateTime
import java.util.logging.Level

class AnimationDigitalNetworkPlatform :
    AbstractPlatform<AnimationDigitalNetworkConfiguration, CountryCode, Array<AbstractAnimationDigitalNetworkWrapper.Video>>() {
    @Inject
    private lateinit var configCacheService: ConfigCacheService

    override fun getPlatform(): Platform = Platform.ANIM

    override fun getConfigurationClass() = AnimationDigitalNetworkConfiguration::class.java

    override suspend fun fetchApiContent(key: CountryCode, zonedDateTime: ZonedDateTime): Array<AbstractAnimationDigitalNetworkWrapper.Video> {
        return AnimationDigitalNetworkWrapper.getLatestVideos(zonedDateTime.toLocalDate())
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            val api = if (bypassFileContent != null && bypassFileContent.exists()) {
                ObjectParser.fromJson(
                    ObjectParser.fromJson(bypassFileContent.readText()).getAsJsonArray("videos"),
                    Array<AbstractAnimationDigitalNetworkWrapper.Video>::class.java
                )
            } else {
                getApiContent(countryCode, zonedDateTime)
            }

            api.forEach {
                try {
                    list.addAll(convertEpisode(countryCode, it, zonedDateTime, needSimulcast = configCacheService.getValueAsBoolean(ConfigPropertyKey.CHECK_SIMULCAST, true)))
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
        video: AbstractAnimationDigitalNetworkWrapper.Video,
        zonedDateTime: ZonedDateTime,
        needSimulcast: Boolean = true,
        checkAnimation: Boolean = true
    ): List<Episode> {
        val season = video.season?.toIntOrNull() ?: 1

        val animeName = (video.show.shortTitle ?: video.show.title)
            .replace("(?: -)? Saison \\d|Part.*|$season$| [${StringUtils.ROMAN_NUMBERS_CHECK}]+$".toRegex(), "")
            .trim()

        if (configuration!!.blacklistedSimulcasts.contains(animeName.lowercase())) throw AnimeException("\"$animeName\" is blacklisted")

        val genres = video.show.genres
        val isConfigurationSimulcasted = containsAnimeSimulcastConfiguration(animeName)

        if ((genres.isEmpty() || !genres.any { it.startsWith("Animation ", true) }) && !isConfigurationSimulcasted && checkAnimation)
            throw Exception("Anime is not an animation")

        val isSimulcasted = video.show.simulcast || video.show.firstReleaseYear in (0..1).map { (zonedDateTime.year - it).toString() } || configCacheService.getValueAsString(ConfigPropertyKey.ANIMATION_DITIGAL_NETWORK_SIMULCAST_DETECTION_REGEX)?.let { Regex(it).containsMatchIn((video.show.summary.normalize() ?: "").lowercase()) } == true

        if (needSimulcast && !(isConfigurationSimulcasted || isSimulcasted))
            throw AnimeNotSimulcastedException("Anime is not simulcasted")

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
                animeImage = video.show.fullHDImage,
                animeBanner = video.show.fullHDBanner,
                animeDescription = video.show.summary.normalize(),
                releaseDateTime = video.releaseDate,
                episodeType = episodeType,
                seasonId = video.season ?: "1",
                season = season,
                number = number,
                duration = video.duration,
                title = video.name.normalize(),
                description = video.summary.normalize(),
                image = video.fullHDImage,
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

        val movieMatch = "Film (\\d*)".toRegex().find(numberAsString ?: "")

        if (movieMatch != null) {
            val movieNumber = movieMatch.groupValues[1].toIntOrNull()
            return (movieNumber ?: number) to EpisodeType.FILM
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

    fun getAnimationDigitalNetworkId(identifier: String) =
        "[A-Z]{2}-ANIM-([0-9]{1,5})-[A-Z]{2}-[A-Z]{2}(?:-UNC)?".toRegex().find(identifier)?.groupValues?.get(1)
}