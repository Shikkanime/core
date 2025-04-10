package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.variants.SeparateVariantDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.*
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.repositories.EpisodeVariantRepository
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.RuleCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.StringUtils
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class EpisodeVariantService : AbstractService<EpisodeVariant, EpisodeVariantRepository>() {
    @Inject private lateinit var episodeVariantRepository: EpisodeVariantRepository
    @Inject private lateinit var simulcastCacheService: SimulcastCacheService
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject private lateinit var ruleCacheService: RuleCacheService
    @Inject private lateinit var ruleService: RuleService
    @Inject private lateinit var attachmentService: AttachmentService

    override fun getRepository() = episodeVariantRepository

    fun findAllTypeIdentifier() = episodeVariantRepository.findAllTypeIdentifier()

    fun findAllByMapping(mappingUUID: UUID) = episodeVariantRepository.findAllByMapping(mappingUUID)

    fun findAllByMapping(mapping: EpisodeMapping) = findAllByMapping(mapping.uuid!!)

    fun findAllVariantReleases(countryCode: CountryCode, member: Member?, startZonedDateTime: ZonedDateTime, endZonedDateTime: ZonedDateTime) =
        episodeVariantRepository.findAllVariantReleases(countryCode, member, startZonedDateTime, endZonedDateTime)

    fun findAllIdentifiers() = episodeVariantRepository.findAllIdentifiers()

    fun findAllVariantsByCountryCodeAndPlatformAndReleaseDateTimeBetween(
        countryCode: CountryCode,
        platform: Platform,
        startZonedDateTime: ZonedDateTime,
        endZonedDateTime: ZonedDateTime
    ) = episodeVariantRepository.findAllVariantsByCountryCodeAndPlatformAndReleaseDateTimeBetween(
        countryCode,
        platform,
        startZonedDateTime,
        endZonedDateTime
    )

    fun getSimulcast(anime: Anime, entity: EpisodeMapping, previousReleaseDateTime: ZonedDateTime? = null, sqlCheck: Boolean = true): Simulcast {
        val simulcastRange = configCacheService.getValueAsInt(ConfigPropertyKey.SIMULCAST_RANGE, 1)
        val adjustedDates = (-simulcastRange..simulcastRange step simulcastRange).map { entity.releaseDateTime.plusDays(it.toLong()) }
        val simulcasts = adjustedDates.map { Simulcast(season = Season.entries[(it.monthValue - 1) / 3], year = it.year) }
        val (previousSimulcast, currentSimulcast, nextSimulcast) = simulcasts
        val isAnimeReleaseDateTimeBeforeMinusXDays = anime.releaseDateTime < adjustedDates.first()

        val diff = (previousReleaseDateTime ?: if (sqlCheck) {
            episodeMappingService.findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime, entity)
        } else null)?.until(entity.releaseDateTime, ChronoUnit.MONTHS) ?: -1

        val chosenSimulcast = when {
            anime.simulcasts.any { it.year == nextSimulcast.year && it.season == nextSimulcast.season } -> nextSimulcast
            entity.number!! <= 1 && currentSimulcast != nextSimulcast -> nextSimulcast
            entity.number!! > 1 && isAnimeReleaseDateTimeBeforeMinusXDays && (diff == -1L || diff >= configCacheService.getValueAsInt(
                ConfigPropertyKey.SIMULCAST_RANGE_DELAY,
                3
            )) -> nextSimulcast
            entity.number!! > 1 && isAnimeReleaseDateTimeBeforeMinusXDays && currentSimulcast != previousSimulcast -> previousSimulcast
            else -> currentSimulcast
        }

        return simulcastCacheService.findBySeasonAndYear(chosenSimulcast.season!!, chosenSimulcast.year!!)
            ?: chosenSimulcast
    }

    /**
     * Saves an `EpisodeVariant` entity based on the provided episode data.
     *
     * @param episode The episode data from the platform to be saved.
     * @param updateMappingDateTime Whether to update the mapping's last release date-time if the episode's release date-time is newer. Default is `true`.
     * @param episodeMapping An optional existing `EpisodeMapping` to associate with the episode. If not provided, a new mapping will be created.
     * @param async Whether to perform certain operations asynchronously. Default is `true`.
     *
     * @return The saved `EpisodeVariant` entity.
     */
    fun save(
        episode: AbstractPlatform.Episode,
        updateMappingDateTime: Boolean = true,
        episodeMapping: EpisodeMapping? = null,
        async: Boolean = true
    ): EpisodeVariant {
        // Get the current date-time
        val now = ZonedDateTime.now()

        // Retrieve all rules associated with the platform, series, and season
        val rules = ruleCacheService.findAllByPlatformSeriesIdAndSeasonId(episode.platform, episode.animeId, episode.seasonId)

        // Apply each rule to modify the episode's properties
        rules.forEach { rule ->
            when (rule.action!!) {
                Rule.Action.REPLACE_ANIME_NAME -> episode.anime = rule.actionValue!!
                Rule.Action.REPLACE_SEASON_NUMBER -> episode.season = rule.actionValue!!.toInt()
                Rule.Action.ADD_TO_NUMBER -> if (episode.number != -1 || episode.episodeType == EpisodeType.EPISODE) {
                    episode.number += rule.actionValue!!.toInt()
                }
            }

            // Update the last usage date-time of the rule
            rule.lastUsageDateTime = now
            ruleService.update(rule)
        }

        // Generate a slug for the anime based on its name
        val animeName = StringUtils.removeAnimeNamePart(episode.anime)
        val slug = StringUtils.toSlug(StringUtils.getShortName(animeName))

        // Create a mapping of anime hash codes to their UUIDs
        val animeHashCodes = animeService.findAllUuidAndSlug().associate { tuple ->
            StringUtils.computeAnimeHashcode(tuple[1] as String) to (tuple[0] as UUID)
        }

        // Find or create the anime entity
        val anime = animeService.findBySlug(episode.countryCode, slug)
            ?: animeService.find(animeHashCodes[StringUtils.computeAnimeHashcode(slug)])
            ?: animeService.save(
                Anime(
                    countryCode = episode.countryCode,
                    name = animeName,
                    releaseDateTime = episode.releaseDateTime,
                    lastReleaseDateTime = episode.releaseDateTime,
                    description = episode.animeDescription,
                    slug = slug
                )
            ).apply {
                // Create attachments for the anime if not in test mode
                if (!Constant.isTest) {
                    attachmentService.createAttachmentOrMarkAsActive(uuid!!, ImageType.THUMBNAIL, url = episode.animeImage, async = async)
                    attachmentService.createAttachmentOrMarkAsActive(uuid, ImageType.BANNER, url = episode.animeBanner, async = async)
                }
            }

        // Save the anime-platform association if it doesn't already exist
        if (animePlatformService.findByAnimePlatformAndId(anime, episode.platform, episode.animeId) == null && (rules.isEmpty() || rules.any { rule -> rule.action != Rule.Action.REPLACE_ANIME_NAME })) {
            animePlatformService.save(
                AnimePlatform(
                    anime = anime,
                    platform = episode.platform,
                    platformId = episode.animeId
                )
            )
        }

        // Retrieve or create the episode mapping
        val mapping = episodeMapping ?: getEpisodeMapping(anime, episode, async)

        // Update the mapping's last release date-time if necessary
        if (updateMappingDateTime && episode.releaseDateTime > mapping.lastReleaseDateTime) {
            mapping.lastReleaseDateTime = episode.releaseDateTime
            episodeMappingService.update(mapping)
        }

        // Update the anime entity with the episode's data
        updateAnime(anime, episode, mapping)

        // Save the `EpisodeVariant` entity
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
                uncensored = episode.uncensored
            )
        )

        // Log the creation of the `EpisodeVariant`
        traceActionService.createTraceAction(savedEntity, TraceAction.Action.CREATE)

        // Return the saved entity
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
        episode: AbstractPlatform.Episode,
        async: Boolean = true
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
            )
        ).apply {
            if (!Constant.isTest)
                attachmentService.createAttachmentOrMarkAsActive(uuid!!, ImageType.BANNER, url = episode.image, async = async)
        }

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

        if (episode.releaseDateTime > anime.lastReleaseDateTime) {
            anime.lastReleaseDateTime = episode.releaseDateTime
            needAnimeUpdate = true
        }

        if (episode.audioLocale != episode.countryCode.locale && episode.episodeType != EpisodeType.FILM) {
            val simulcast = getSimulcast(anime, mapping)
            val added = animeService.addSimulcastToAnime(anime, simulcast)
            needAnimeUpdate = needAnimeUpdate || added
        }

        if (needAnimeUpdate) {
            animeService.update(anime)
        }
    }

    fun separate(uuid: UUID, dto: SeparateVariantDto) {
        val episodeVariant = find(uuid) ?: return
        val mapping = episodeVariant.mapping!!

        val entity = EpisodeMapping(
            anime = mapping.anime,
            releaseDateTime = mapping.releaseDateTime,
            lastUpdateDateTime = ZonedDateTime.parse("2000-01-01T00:00:00Z"),
            episodeType = dto.episodeType,
            season = dto.season,
            number = dto.number,
            duration = mapping.duration,
            title = mapping.title,
            description = mapping.description,
        )

        val existing = episodeMappingService.findByAnimeSeasonEpisodeTypeNumber(
            mapping.anime!!.uuid!!,
            dto.season,
            dto.episodeType,
            dto.number
        )

        if (existing != null) {
            episodeVariant.mapping = existing
            update(episodeVariant)
            return
        } else {
            val image = attachmentService.findByEntityUuidTypeAndActive(mapping.uuid!!, ImageType.BANNER)
            val saved = episodeMappingService.save(entity)
            attachmentService.createAttachmentOrMarkAsActive(saved.uuid!!, ImageType.BANNER, url = image?.url)
            episodeVariant.mapping = saved
            update(episodeVariant)
        }
    }
}
