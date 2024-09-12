package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.repositories.EpisodeVariantRepository
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class EpisodeVariantService : AbstractService<EpisodeVariant, EpisodeVariantRepository>() {
    @Inject
    private lateinit var episodeVariantRepository: EpisodeVariantRepository

    @Inject
    private lateinit var simulcastService: SimulcastService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var traceActionService: TraceActionService

    override fun getRepository() = episodeVariantRepository

    fun findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleByDateRange(
        countryCode: CountryCode,
        member: Member?,
        start: ZonedDateTime,
        end: ZonedDateTime,
    ) = episodeVariantRepository.findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleByDateRange(
        countryCode,
        member,
        start,
        end
    )

    fun findAllIdentifierByDateRangeWithoutNextEpisode(
        countryCode: CountryCode,
        start: ZonedDateTime,
        end: ZonedDateTime,
        platform: Platform
    ) = episodeVariantRepository.findAllIdentifierByDateRangeWithoutNextEpisode(countryCode, start, end, platform)

    fun findAllTypeIdentifier() = episodeVariantRepository.findAllTypeIdentifier()

    fun findAudioLocalesAndSeasonsByAnime(anime: Anime) = episodeVariantRepository.findAudioLocalesAndSeasonsByAnime(anime)

    fun findAllByAnime(anime: Anime) = episodeVariantRepository.findAllByAnime(anime)

    fun findAllByMapping(mapping: EpisodeMapping) = episodeVariantRepository.findAllByMapping(mapping)

    fun findAllIdentifiers() = episodeVariantRepository.findAllIdentifiers()

    fun findByIdentifier(identifier: String) = episodeVariantRepository.findByIdentifier(identifier)

    fun getSimulcast(anime: Anime, entity: EpisodeMapping): Simulcast {
        val simulcastRange = configCacheService.getValueAsInt(ConfigPropertyKey.SIMULCAST_RANGE, 1)

        val adjustedDates = listOf(-simulcastRange, 0, simulcastRange).map { days ->
            entity.releaseDateTime.plusDays(days.toLong())
        }

        val simulcasts = adjustedDates.map {
            Simulcast(season = Constant.seasons[(it.monthValue - 1) / 3], year = it.year)
        }

        val previousSimulcast = simulcasts[0]
        val currentSimulcast = simulcasts[1]
        val nextSimulcast = simulcasts[2]

        val isAnimeReleaseDateTimeBeforeMinusXDays = anime.releaseDateTime.isBefore(adjustedDates[0])

        val diff = episodeMappingService.findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime, entity)
            ?.until(entity.releaseDateTime, ChronoUnit.MONTHS) ?: -1

        val choosenSimulcast = when {
            anime.simulcasts.any { it.year == nextSimulcast.year && it.season == nextSimulcast.season } -> nextSimulcast
            entity.number!! <= 1 && currentSimulcast != nextSimulcast -> nextSimulcast
            entity.number!! > 1 && isAnimeReleaseDateTimeBeforeMinusXDays && (diff == -1L || diff >= configCacheService.getValueAsInt(
                ConfigPropertyKey.SIMULCAST_RANGE_DELAY,
                3
            )) -> nextSimulcast

            entity.number!! > 1 && isAnimeReleaseDateTimeBeforeMinusXDays && currentSimulcast != previousSimulcast -> previousSimulcast
            else -> currentSimulcast
        }

        return simulcastService.findBySeasonAndYear(choosenSimulcast.season!!, choosenSimulcast.year!!)
            ?: choosenSimulcast
    }

    fun save(episode: AbstractPlatform.Episode, updateMappingDateTime: Boolean = true, episodeMapping: EpisodeMapping? = null): EpisodeVariant {
        val anime =
            animeService.findBySlug(episode.countryCode, StringUtils.toSlug(StringUtils.getShortName(episode.anime)))
                ?: animeService.save(
                    Anime(
                        countryCode = episode.countryCode,
                        name = episode.anime,
                        releaseDateTime = episode.releaseDateTime,
                        lastReleaseDateTime = episode.releaseDateTime,
                        image = episode.animeImage,
                        banner = episode.animeBanner,
                        description = episode.animeDescription,
                        slug = StringUtils.toSlug(StringUtils.getShortName(episode.anime))
                    ).apply {
                        status = StringUtils.getStatus(this)
                    }
                )

        val mapping = episodeMapping ?: getEpisodeMapping(anime, episode)

        if (updateMappingDateTime) {
            mapping.lastReleaseDateTime = episode.releaseDateTime
            mapping.lastUpdateDateTime = episode.releaseDateTime
            episodeMappingService.update(mapping)
        }

        updateAnime(anime, episode, mapping)

        val savedEntity = super.save(
            EpisodeVariant(
                mapping = mapping,
                releaseDateTime = episode.releaseDateTime,
                platform = episode.platform,
                audioLocale = episode.audioLocale,
                identifier = StringUtils.getIdentifier(
                    episode.countryCode,
                    episode.platform,
                    episode.id,
                    episode.audioLocale,
                    episode.uncensored
                ),
                url = episode.url,
                uncensored = episode.uncensored,
            )
        )

        MapCache.invalidate(EpisodeMapping::class.java, EpisodeVariant::class.java)
        traceActionService.createTraceAction(savedEntity, TraceAction.Action.CREATE)
        return savedEntity
    }

    override fun update(entity: EpisodeVariant): EpisodeVariant {
        traceActionService.createTraceAction(entity, TraceAction.Action.UPDATE)
        return super.update(entity)
    }

    override fun delete(entity: EpisodeVariant) {
        super.delete(entity)
        traceActionService.createTraceAction(entity, TraceAction.Action.DELETE)
    }

    private fun getEpisodeMapping(
        anime: Anime,
        episode: AbstractPlatform.Episode
    ): EpisodeMapping {
        var mapping = episodeMappingService.findByAnimeSeasonEpisodeTypeNumber(
            anime.uuid!!,
            episode.season,
            episode.episodeType,
            episode.number
        ) ?: episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = episode.releaseDateTime,
                lastReleaseDateTime = episode.releaseDateTime,
                lastUpdateDateTime = episode.releaseDateTime,
                episodeType = episode.episodeType,
                season = episode.season,
                number = episode.number,
                duration = episode.duration,
                title = episode.title,
                description = episode.description?.take(Constant.MAX_DESCRIPTION_LENGTH),
                image = episode.image,
            ).apply {
                status = StringUtils.getStatus(this)
            }
        )

        episode.number.takeIf { it == -1 }?.let {
            val number = episodeMappingService.findLastNumber(
                anime,
                episode.episodeType,
                episode.season,
                episode.platform,
                episode.audioLocale
            ) + 1

            val newCheck = episodeMappingService.findByAnimeSeasonEpisodeTypeNumber(
                anime.uuid,
                episode.season,
                episode.episodeType,
                number
            )

            if (newCheck != null) {
                if (findAllByMapping(mapping).isEmpty()) {
                    episodeMappingService.delete(mapping)
                }

                mapping = newCheck
            } else {
                mapping.number = number
                episodeMappingService.update(mapping)
            }

            episode.number = mapping.number!!
        }

        return mapping
    }

    private fun updateAnime(
        anime: Anime,
        episode: AbstractPlatform.Episode,
        mapping: EpisodeMapping
    ) {
        var needAnimeUpdate = false

        if (anime.banner.isNullOrBlank() && episode.animeBanner.isNotBlank()) {
            anime.banner = episode.animeBanner
            needAnimeUpdate = true
        }

        if (anime.description.isNullOrBlank() && !episode.animeDescription.isNullOrBlank()) {
            anime.description = episode.animeDescription
            needAnimeUpdate = true
        }

        if (anime.lastReleaseDateTime.isBefore(episode.releaseDateTime)) {
            anime.lastReleaseDateTime = episode.releaseDateTime
            needAnimeUpdate = true
        }

        if (episode.audioLocale != episode.countryCode.locale && episode.episodeType != EpisodeType.FILM) {
            val simulcast = getSimulcast(anime, mapping)
            animeService.addSimulcastToAnime(anime, simulcast)
            needAnimeUpdate = true
        }

        if (needAnimeUpdate) {
            anime.status = StringUtils.getStatus(anime)
            animeService.update(anime)
            MapCache.invalidate(Anime::class.java)
        }
    }
}