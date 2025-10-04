package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.entities.enums.*
import fr.shikkanime.exceptions.AnimeException
import fr.shikkanime.exceptions.AnimeNotSimulcastedException
import fr.shikkanime.exceptions.NotSimulcastedMediaException
import fr.shikkanime.platforms.configuration.AnimationDigitalNetworkConfiguration
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.AnimePlatformCacheService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.normalize
import fr.shikkanime.wrappers.factories.AbstractAnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.impl.AnimationDigitalNetworkWrapper
import java.io.File
import java.time.ZonedDateTime
import java.util.logging.Level

private val MOVIE_REGEX = "Film(?: (\\d*))?".toRegex()
private val SPECIAL_EPISODE_REGEX = "(?:Épisode spécial|OAV)(?: (\\d*))?".toRegex()
private val NUMBER_CLEANUP_REGEX = "\\(.*\\)".toRegex()

private const val SHOW_TYPE_MOVIE = "MOV"
private const val SHOW_TYPE_OAV = "OAV"

class AnimationDigitalNetworkPlatform :
    AbstractPlatform<AnimationDigitalNetworkConfiguration, CountryCode, Array<AbstractAnimationDigitalNetworkWrapper.Video>>() {
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var animeCacheService: AnimeCacheService
    @Inject private lateinit var animePlatformCacheService: AnimePlatformCacheService

    override fun getPlatform(): Platform = Platform.ANIM

    override fun getConfigurationClass() = AnimationDigitalNetworkConfiguration::class.java

    private fun cleanAnimeName(title: String, season: String?): String {
        val seasonPattern = season?.toIntOrNull() ?: 1
        // Regex to remove season/part indicators and roman numerals from the end
        val regex = "(?: -)? Saison \\d+|Part .*| $seasonPattern$| [${StringUtils.ROMAN_NUMBERS_CHECK}]+$".toRegex()
        return title.replace(regex, StringUtils.EMPTY_STRING).trim()
    }

    override suspend fun fetchApiContent(key: CountryCode, zonedDateTime: ZonedDateTime): Array<AbstractAnimationDigitalNetworkWrapper.Video> {
        val latestVideos = AnimationDigitalNetworkWrapper.getLatestVideos(zonedDateTime.toLocalDate()).toMutableList()

        latestVideos.addAll(
            configuration?.simulcasts
                ?.filter { it.audioLocaleDelay == zonedDateTime.dayOfWeek.value }
                ?.flatMap { simulcast ->
                    animeCacheService.findByName(key, simulcast.name)?.let { anime ->
                        animePlatformCacheService.findAllIdByAnimeAndPlatform(anime.uuid!!, getPlatform())
                            .flatMap { platformId ->
                                platformId.toIntOrNull()?.let { id ->
                                    AnimationDigitalNetworkWrapper.getShowVideos(id)
                                        .filter { it.releaseDate != null && it.releaseDate!!.toLocalTime() >= zonedDateTime.toLocalTime() }
                                        .onEach { it.releaseDate = zonedDateTime }
                                        .toList()
                                } ?: emptyList()
                            }
                    } ?: emptyList()
                } ?: emptyList()
        )

        return latestVideos.toTypedArray()
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
        val animeName = cleanAnimeName(video.show.shortTitle?.takeIf { it.isNotBlank() } ?: video.show.title, season.toString())

        if (isBlacklisted(animeName.lowercase())) throw AnimeException("\"$animeName\" is blacklisted")

        val genres = video.show.genres
        val isConfigurationSimulcasted = containsAnimeSimulcastConfiguration(animeName)

        if ((genres.isEmpty() || !genres.any { it.startsWith("Animation ", true) }) && !isConfigurationSimulcasted && checkAnimation)
            throw Exception("Anime is not an animation")

        val isSimulcasted = video.show.simulcast || video.show.firstReleaseYear in (0..1).map { (zonedDateTime.year - it).toString() } || configCacheService.getValueAsString(ConfigPropertyKey.ANIMATION_DITIGAL_NETWORK_SIMULCAST_DETECTION_REGEX)?.let { Regex(it).containsMatchIn((video.show.summary.normalize() ?: StringUtils.EMPTY_STRING).lowercase()) } == true

        if (needSimulcast && !(isConfigurationSimulcasted || isSimulcasted))
            throw AnimeNotSimulcastedException("Anime is not simulcasted")

        val trailerIndicators = listOf("Bande-annonce", "Bande annonce", "Court-métrage", "Opening", "Making-of")
        val specialShowTypes = listOf("PV", "BONUS")

        if (trailerIndicators.any { video.shortNumber?.startsWith(it) == true } || video.type in specialShowTypes)
            throw NotSimulcastedMediaException("Trailer or special show type")

        val (number, episodeType) = getNumberAndEpisodeType(video.shortNumber, video.type)

        return video.languages.map {
            Episode(
                countryCode = countryCode,
                animeId = video.show.id.toString(),
                anime = animeName,
                animeAttachments = mapOf(
                    ImageType.THUMBNAIL to video.show.fullHDImage,
                    ImageType.BANNER to video.show.fullHDBanner,
                    ImageType.CAROUSEL to video.show.fullHDCarousel
                ),
                animeDescription = video.show.summary.normalize(),
                releaseDateTime = requireNotNull(video.releaseDate) { "Release date is null" },
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

    private fun parseInitialNumber(rawString: String?): Int {
        return rawString?.replace(NUMBER_CLEANUP_REGEX, StringUtils.EMPTY_STRING)?.trim()?.toIntOrNull() ?: -1
    }

    /**
     * Determines the episode number and its corresponding type based on the raw episode string and show type.
     *
     * @param rawEpisodeString the raw string representing the episode, which may contain details like episode number or type
     * @param showType the type of the show, which can indicate if the content is a movie, special, or regular episode
     * @return a pair containing the episode number as an integer and the corresponding episode type as an [EpisodeType] enum value
     */
    private fun getNumberAndEpisodeType(rawEpisodeString: String?, showType: String?): Pair<Int, EpisodeType> {
        val initialNumber = parseInitialNumber(rawEpisodeString)

        val movieMatch = rawEpisodeString?.let { MOVIE_REGEX.find(it) }
        if (movieMatch != null || showType == SHOW_TYPE_MOVIE) {
            val filmNumber = movieMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
            return (filmNumber ?: initialNumber) to EpisodeType.FILM
        }

        val specialEpisodeMatch = rawEpisodeString?.let { SPECIAL_EPISODE_REGEX.find(it) }
        if (specialEpisodeMatch != null || showType == SHOW_TYPE_OAV || rawEpisodeString?.contains(".") == true) {
            val specialNumber = specialEpisodeMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
            return (specialNumber ?: initialNumber) to EpisodeType.SPECIAL
        }

        return initialNumber to EpisodeType.EPISODE
    }

    private fun getAudioLocale(string: String): String {
        return when (string) {
            "vostf" -> "ja-JP"
            "vf" -> "fr-FR"
            else -> throw Exception("Language is null")
        }
    }
}