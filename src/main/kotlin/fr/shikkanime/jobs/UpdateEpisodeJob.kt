package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.EpisodeNoSubtitlesOrVoiceException
import fr.shikkanime.platforms.AbstractPlatform.Episode
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.caches.LanguageCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.CrunchyrollWrapper
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import java.util.logging.Level

class UpdateEpisodeJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var languageCacheService: LanguageCacheService

    override fun run() {
        // Take 15 episodes of a platform, and if the lastUpdate is older than 30 days, or if the episode mapping is valid
        val lastDateTime = ZonedDateTime.now().minusDays(30)
        val adnEpisodes = episodeMappingService.findAllNeedUpdateByPlatform(Platform.ANIM, lastDateTime)
        val crunchyrollEpisodes = episodeMappingService.findAllNeedUpdateByPlatform(Platform.CRUN, lastDateTime)

        logger.info("Found ${adnEpisodes.size} ADN episodes and ${crunchyrollEpisodes.size} Crunchyroll episodes to update")

        val needUpdateEpisodes = (adnEpisodes + crunchyrollEpisodes).distinctBy { it.uuid }
            .shuffled()
            .take(15)
        val accessToken = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }

        needUpdateEpisodes.forEach { mapping ->
            val variants = episodeVariantService.findAllByMapping(mapping)
            val mappingIdentifier = "${StringUtils.getShortName(mapping.anime!!.name!!)} - S${mapping.season} ${mapping.episodeType} ${mapping.number}"
            logger.info("Updating episode $mappingIdentifier...")
            val episodes = variants.flatMap { variant -> runBlocking { a(accessToken, mapping, variant) } }
            val list = variants.map { it.identifier }

            episodes.filter { it.getIdentifier() !in list }.takeIf { it.isNotEmpty() }
                ?.also { logger.info("Found ${it.size} new episodes for $mappingIdentifier") }
                ?.map { episode -> episodeVariantService.save(episode, false, mapping) }
                ?.also { logger.info("Added ${it.size} episodes for $mappingIdentifier") }

            val episode =
                episodes.sortedWith(compareBy({ it.releaseDateTime }, { it.uncensored })).firstOrNull { StringUtils.getStatus(it) == Status.VALID } ?: run {
                    logger.log(Level.WARNING, "No valid episode found for $mappingIdentifier")
                    return@forEach
                }

            if (episode.image != Constant.DEFAULT_IMAGE_PREVIEW && mapping.image !in episodes.map { it.image }) {
                mapping.image = episode.image
                episodeMappingService.addImage(mapping.uuid!!, episode.image, true)
                logger.info("Image updated for $mappingIdentifier to ${episode.image}")
            }

            if (episode.title != mapping.title && !episode.title.isNullOrBlank()) {
                mapping.title = episode.title
                logger.info("Title updated for $mappingIdentifier to ${episode.title}")
            }

            if (episode.description != mapping.description && !episode.description.isNullOrBlank() && languageCacheService.detectLanguage(episode.description) == mapping.anime!!.countryCode!!.name.lowercase()) {
                mapping.description = episode.description
                logger.info("Description updated for $mappingIdentifier to ${episode.description}")
            }

            mapping.status = StringUtils.getStatus(mapping)
            mapping.lastUpdateDateTime = ZonedDateTime.now()
            episodeMappingService.update(mapping)
            logger.info("Episode $mappingIdentifier updated")
        }
    }

    suspend fun a(accessToken: String, episodeMapping: EpisodeMapping, episodeVariant: EpisodeVariant): List<Episode> {
        val countryCode = episodeMapping.anime!!.countryCode!!
        val episodes = mutableListOf<Episode>()

        if (episodeVariant.platform == Platform.ANIM) {
            val adnId = "[A-Z]{2}-ANIM-([0-9]{5})-[A-Z]{2}-[A-Z]{2}(?:-UNC)?".toRegex().find(episodeVariant.identifier!!)?.groupValues?.get(1)
            episodes.addAll(getADNEpisodeAndVariants(countryCode, adnId!!))
        }

        if (episodeVariant.platform == Platform.CRUN) {
            val crunchyrollId = "[A-Z]{2}-CRUN-([A-Z0-9]{9})-[A-Z]{2}-[A-Z]{2}".toRegex().find(episodeVariant.identifier!!)?.groupValues?.get(1)
            episodes.addAll(getCrunchyrollEpisodeAndVariants(countryCode, accessToken, crunchyrollId!!))
        }

        return episodes
    }

    private suspend fun getCrunchyrollEpisodeAndVariants(
        countryCode: CountryCode,
        accessToken: String,
        crunchyrollId: String,
    ): List<Episode> {
        val episodes = mutableListOf<Episode>()
        val crunchyrollEpisode = CrunchyrollWrapper.getEpisode(countryCode.locale, accessToken, crunchyrollId)
        val series = CrunchyrollWrapper.getSeries(countryCode.locale, accessToken, crunchyrollEpisode.seriesId)

        val crunchyrollEpisodes = (crunchyrollEpisode.versions ?: emptyList())
            .map { runBlocking { CrunchyrollWrapper.getEpisode(countryCode.locale, accessToken, it.guid) } }
            .plus(crunchyrollEpisode)
            .distinctBy { it.id }

        crunchyrollEpisodes.forEach { episode ->
            try {
                val animeImage = series.images.posterTall.first().maxByOrNull { poster -> poster.width }?.source?.takeIf { it.isNotBlank() }
                    ?: throw Exception("Image is null or empty")
                val animeBanner = series.images.posterWide.first().maxByOrNull { poster -> poster.width }?.source?.takeIf { it.isNotBlank() }
                    ?: throw Exception("Banner is null or empty")

                val isDubbed = episode.audioLocale == countryCode.locale
                val season = episode.seasonNumber ?: 1
                val (number, episodeType) = getNumberAndEpisodeTypeByCrunchyrollEpisode(episode)
                val url = CrunchyrollWrapper.buildUrl(countryCode, episode.id!!, episode.slugTitle)
                val image = episode.images?.thumbnail?.get(0)?.maxByOrNull { it.width }?.source?.takeIf { it.isNotBlank() } ?: Constant.DEFAULT_IMAGE_PREVIEW
                val duration = episode.durationMs / 1000
                val description = episode.description?.replace('\n', ' ')?.takeIf { it.isNotBlank() }

                if (!isDubbed && (episode.subtitleLocales.isEmpty() || !episode.subtitleLocales.contains(countryCode.locale)))
                    throw EpisodeNoSubtitlesOrVoiceException("Episode is not available in ${countryCode.name} with subtitles or voice")

                episodes.add(
                    Episode(
                        countryCode = countryCode,
                        anime = series.title,
                        animeImage = animeImage,
                        animeBanner = animeBanner,
                        animeDescription = series.description,
                        releaseDateTime = episode.premiumAvailableDate,
                        episodeType = episodeType,
                        season = season,
                        number = number,
                        duration = duration,
                        title = episode.title,
                        description = description,
                        image = image,
                        platform = Platform.CRUN,
                        audioLocale = episode.audioLocale,
                        id = episode.id,
                        url = url,
                        uncensored = false,
                    )
                )
            } catch (e: Exception) {
                logger.warning("Error while getting Crunchyroll episode ${episode.id} : ${e.message}")
            }
        }

        return episodes
    }

    private fun getNumberAndEpisodeTypeByCrunchyrollEpisode(episode: CrunchyrollWrapper.Episode): Pair<Int, EpisodeType> {
        var number = episode.number ?: -1
        val specialEpisodeRegex = "SP(\\d*)".toRegex()

        var episodeType = when {
            episode.seasonSlugTitle?.contains("movie", true) == true -> EpisodeType.FILM
            number == -1 -> EpisodeType.SPECIAL
            else -> EpisodeType.EPISODE
        }

        specialEpisodeRegex.find(episode.numberString)?.let {
            episodeType = EpisodeType.SPECIAL
            number = it.groupValues[1].toIntOrNull() ?: -1
        }

        return Pair(number, episodeType)
    }

    private suspend fun getADNEpisodeAndVariants(
        countryCode: CountryCode,
        adnId: String,
    ): List<Episode> {
        val video = AnimationDigitalNetworkWrapper.getShowVideo(adnId)

        try {
            val season = video.season?.toIntOrNull() ?: 1

            var animeName = video.show.shortTitle ?: video.show.title
            animeName = animeName.replace(Regex("Saison \\d"), "").trim()
            animeName = animeName.replace(season.toString(), "").trim()
            animeName = animeName.replace(Regex(" -.*"), "").trim()
            animeName = animeName.replace(Regex(" Part.*"), "").trim()

            val animeDescription = video.show.summary?.replace('\n', ' ') ?: ""
            val genres = video.show.genres

            if ((genres.isEmpty() || !genres.any { it.startsWith("Animation ", true) }))
                throw Exception("Anime is not an animation")

            val trailerIndicators = listOf("Bande-annonce", "Bande annonce", "Court-métrage", "Opening", "Making-of")
            val specialShowTypes = listOf("PV", "BONUS")

            if (trailerIndicators.any { video.shortNumber?.startsWith(it) == true } || specialShowTypes.contains(video.type)) {
                throw Exception("Anime is not an episode")
            }

            val (number, episodeType) = getNumberAndEpisodeTypeByADNEpisode(video.shortNumber, video.type)

            val description = video.summary?.replace('\n', ' ')?.ifBlank { null }

            return video.languages.map {
                Episode(
                    countryCode = countryCode,
                    anime = animeName,
                    animeImage = video.show.image2x,
                    animeBanner = video.show.imageHorizontal2x,
                    animeDescription = animeDescription,
                    releaseDateTime = video.releaseDate,
                    episodeType = episodeType,
                    season = season,
                    number = number,
                    duration = video.duration,
                    title = video.name?.ifBlank { null },
                    description = description,
                    image = video.image2x,
                    platform = Platform.ANIM,
                    audioLocale = getAudioLocale(it),
                    id = video.id.toString(),
                    url = video.url,
                    uncensored = video.title.contains("(NC)"),
                )
            }
        } catch (e: Exception) {
            logger.warning("Error while getting ADN episode $adnId : ${e.message}")
            return emptyList()
        }
    }

    private fun getNumberAndEpisodeTypeByADNEpisode(numberAsString: String?, showType: String?): Pair<Int, EpisodeType> {
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