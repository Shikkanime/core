package fr.shikkanime.platforms

import com.google.inject.Inject
import fr.shikkanime.entities.enums.*
import fr.shikkanime.exceptions.AnimeException
import fr.shikkanime.exceptions.AnimeNotSimulcastedException
import fr.shikkanime.exceptions.NotSimulcastedMediaException
import fr.shikkanime.platforms.configuration.AnimationDigitalNetworkConfiguration
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.takeIfNotBlank
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

private val LOCALE_MAP = mapOf(
    "vostf" to Locale.JA_JP.code,
    "vf" to Locale.FR_FR.code
)

class AnimationDigitalNetworkPlatform :
    AbstractPlatform<AnimationDigitalNetworkConfiguration, CountryCode, Array<AbstractAnimationDigitalNetworkWrapper.Episode>>() {
    @Inject private lateinit var animeCacheService: AnimeCacheService

    override fun getPlatform(): Platform = Platform.ANIM

    override fun getConfigurationClass() = AnimationDigitalNetworkConfiguration::class.java

    private fun cleanAnimeName(title: String, season: String?): String {
        val seasonPattern = season?.toIntOrNull() ?: 1
        // Regex to remove season/part indicators and roman numerals from the end
        val regex = "(?: -)? Saison \\d+|Part .*| $seasonPattern$| [${StringUtils.ROMAN_NUMBERS_CHECK}]+$".toRegex()
        return title.replace(regex, StringUtils.EMPTY_STRING).trim()
    }

    override suspend fun fetchApiContent(key: CountryCode, zonedDateTime: ZonedDateTime): Array<AbstractAnimationDigitalNetworkWrapper.Episode> {
        val latestEpisodes = AnimationDigitalNetworkWrapper.getLatestEpisodes(key.locale, zonedDateTime.toLocalDate()).toMutableList()

        latestEpisodes.addAll(
            configuration?.simulcasts
                ?.filter { it.audioLocaleDelay == zonedDateTime.dayOfWeek.value }
                ?.flatMap { simulcast ->
                    animeCacheService.findByName(key, simulcast.name)?.let { anime ->
                        animePlatformCacheService.findAllIdByAnimeAndPlatform(anime.uuid!!, getPlatform())
                            .flatMap { platformId ->
                                platformId.toIntOrNull()?.let { id ->
                                    AnimationDigitalNetworkWrapper.getEpisodesByShowId(key.locale, id)
                                        .filter { it.releaseDate != null && it.releaseDate!!.toLocalTime() >= zonedDateTime.toLocalTime() }
                                        .onEach { it.releaseDate = zonedDateTime }
                                        .toList()
                                } ?: emptyList()
                            }
                    } ?: emptyList()
                } ?: emptyList()
        )

        return latestEpisodes.toTypedArray()
    }

    override suspend fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            val api = bypassFileContent?.takeIf { it.exists() }?.let {
                ObjectParser.fromJson(
                    ObjectParser.fromJson(bypassFileContent.readText()).getAsJsonArray("videos"),
                    Array<AbstractAnimationDigitalNetworkWrapper.Episode>::class.java
                )
            } ?: getApiContent(countryCode, zonedDateTime)

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

    suspend fun convertEpisode(
        countryCode: CountryCode,
        episode: AbstractAnimationDigitalNetworkWrapper.Episode,
        zonedDateTime: ZonedDateTime,
        needSimulcast: Boolean = true,
        checkAnimation: Boolean = true
    ): List<Episode> {
        val season = episode.season?.toIntOrNull() ?: 1
        val animeName = cleanAnimeName(episode.show.shortTitle?.takeIfNotBlank() ?: episode.show.title, season.toString())

        if (isBlacklisted(animeName)) throw AnimeException("\"$animeName\" is blacklisted")

        val genres = episode.show.genres
        val isConfigurationSimulcasted = containsAnimeSimulcastConfiguration(animeName)

        if ((genres.isEmpty() || !genres.any { it.startsWith("Animation ", true) }) && !isConfigurationSimulcasted && checkAnimation)
            throw Exception("Anime is not an animation")

        val isSimulcasted =
            episode.show.simulcast || episode.show.firstReleaseYear in (0..1).map { (zonedDateTime.year - it).toString() } || configCacheService.getValueAsString(
                ConfigPropertyKey.ANIMATION_DITIGAL_NETWORK_SIMULCAST_DETECTION_REGEX
            )?.let { Regex(it).containsMatchIn((episode.show.summary ?: StringUtils.EMPTY_STRING).lowercase()) } == true

        if (needSimulcast && !(isConfigurationSimulcasted || isSimulcasted))
            throw AnimeNotSimulcastedException("Anime is not simulcasted")

        val trailerIndicators = listOf("Bande-annonce", "Bande annonce", "Court-métrage", "Opening", "Making-of")
        val specialShowTypes = listOf("PV", "BONUS")

        if (trailerIndicators.any { episode.shortNumber?.startsWith(it) == true } || episode.type in specialShowTypes)
            throw NotSimulcastedMediaException("Trailer or special show type")

        val (number, episodeType) = getNumberAndEpisodeType(episode.shortNumber, episode.type)

        return episode.languages.map {
            Episode(
                countryCode = countryCode,
                animeId = episode.show.id.toString(),
                anime = animeName,
                animeAttachments = mapOf(
                    ImageType.THUMBNAIL to episode.show.fullHDImage,
                    ImageType.BANNER to episode.show.fullHDBanner,
                    ImageType.CAROUSEL to episode.show.fullHDCarousel,
                    ImageType.TITLE to episode.show.fullHDTitle,
                ),
                animeDescription = episode.show.summary,
                releaseDateTime = requireNotNull(episode.releaseDate) { "Release date is null" },
                episodeType = episodeType,
                seasonId = episode.season ?: "1",
                season = season,
                number = number,
                duration = episode.duration,
                title = episode.name,
                description = episode.summary,
                image = episode.fullHDImage.takeIf { image -> image.contains("/video/") } ?: Constant.DEFAULT_IMAGE_PREVIEW,
                platform = getPlatform(),
                audioLocale = requireNotNull(LOCALE_MAP[it]) { "Language not supported" },
                id = episode.id.toString(),
                url = episode.url,
                uncensored = episode.title.contains("(NC)", true) || episode.title.contains("Non censuré", true),
                original = episode.languages.size == 1 || episode.languages.indexOf(it) == 0
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

        val movieMatch = rawEpisodeString?.let(MOVIE_REGEX::find)
        if (movieMatch != null || showType == SHOW_TYPE_MOVIE) {
            val filmNumber = movieMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
            return (filmNumber ?: initialNumber) to EpisodeType.FILM
        }

        val specialEpisodeMatch = rawEpisodeString?.let(SPECIAL_EPISODE_REGEX::find)
        if (specialEpisodeMatch != null || showType == SHOW_TYPE_OAV || rawEpisodeString?.contains(".") == true) {
            val specialNumber = specialEpisodeMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
            return (specialNumber ?: initialNumber) to EpisodeType.SPECIAL
        }

        return initialNumber to EpisodeType.EPISODE
    }
}