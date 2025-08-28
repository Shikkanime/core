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

    fun findAllVariantReleases(countryCode: CountryCode, member: Member?, startZonedDateTime: ZonedDateTime, endZonedDateTime: ZonedDateTime, searchTypes: Array<LangType>? = null) =
        episodeVariantRepository.findAllVariantReleases(countryCode, member, startZonedDateTime, endZonedDateTime, searchTypes)

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

    fun findByIdentifier(identifier: String) = episodeVariantRepository.findByIdentifier(identifier)

    /**
     * Determines the appropriate simulcast for a given anime and episode mapping.
     *
     * @param anime The `Anime` entity for which the simulcast is being determined.
     * @param entity The `EpisodeMapping` entity containing episode details.
     * @param previousReleaseDateTime An optional `ZonedDateTime` representing the previous release date-time of the episode. Default is `null`.
     * @param sqlCheck A boolean indicating whether to perform SQL checks for the previous release date-time. Default is `true`.
     *
     * @return The determined `Simulcast` entity.
     */
    fun getSimulcast(anime: Anime, entity: EpisodeMapping, previousReleaseDateTime: ZonedDateTime? = null, sqlCheck: Boolean = true): Simulcast {
        // Retrieve the simulcast range configuration value
        val simulcastRange = configCacheService.getValueAsInt(ConfigPropertyKey.SIMULCAST_RANGE, 1)

        // Generate a list of adjusted dates based on the simulcast range
        val adjustedDates = (-simulcastRange..simulcastRange step simulcastRange).map { entity.releaseDateTime.plusDays(it.toLong()) }

        // Map the adjusted dates to their corresponding simulcast seasons and years
        val simulcasts = adjustedDates.map { Simulcast(season = Season.entries[(it.monthValue - 1) / 3], year = it.year) }

        // Extract the previous, current, and next simulcasts
        val (previousSimulcast, currentSimulcast, nextSimulcast) = simulcasts

        // Check if the anime's release date-time is before the earliest adjusted date
        val isAnimeReleaseDateTimeBeforeMinusXDays = anime.releaseDateTime < adjustedDates.first()

        // Calculate the difference in months between the previous release date-time and the current release date-time
        val diff = (previousReleaseDateTime ?: if (sqlCheck) {
            episodeMappingService.findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime, entity)
        } else null)?.until(entity.releaseDateTime, ChronoUnit.MONTHS) ?: -1

        // Determine the appropriate simulcast based on various conditions
        val chosenSimulcast = when {
            // If the next simulcast already exists for the anime, choose it
            anime.simulcasts.any { it.year == nextSimulcast.year && it.season == nextSimulcast.season } -> nextSimulcast

            // If the episode number is 1 and the current simulcast is not the next simulcast, choose the next simulcast
            entity.number!! <= 1 && currentSimulcast != nextSimulcast -> nextSimulcast

            // If the episode number is greater than 1 and the anime's release date-time is before the earliest adjusted date,
            // and the difference in months is either -1 or greater than or equal to the configured delay, choose the next simulcast
            entity.number!! > 1 && isAnimeReleaseDateTimeBeforeMinusXDays && (diff == -1L || diff >= configCacheService.getValueAsInt(
                ConfigPropertyKey.SIMULCAST_RANGE_DELAY,
                3
            )) -> nextSimulcast

            // If the episode number is greater than 1 and the anime's release date-time is before the earliest adjusted date,
            // and the current simulcast is not the previous simulcast, choose the previous simulcast
            entity.number!! > 1 && isAnimeReleaseDateTimeBeforeMinusXDays && currentSimulcast != previousSimulcast -> previousSimulcast

            // Otherwise, choose the current simulcast
            else -> currentSimulcast
        }

        // Retrieve the simulcast from the cache or return the chosen simulcast
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
            .sortedBy { it.action?.ordinal ?: 0 }

        // Apply each rule to modify the episode's properties
        rules.forEach { rule ->
            when (rule.action!!) {
                Rule.Action.REPLACE_ANIME_NAME -> episode.anime = rule.actionValue!!
                Rule.Action.REPLACE_SEASON_NUMBER -> episode.season = rule.actionValue!!.toInt()
                Rule.Action.REPLACE_EPISODE_TYPE -> episode.episodeType = EpisodeType.valueOf(rule.actionValue!!)
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

        // Find or create the anime entity
        val anime = animeService.findBySlug(episode.countryCode, slug)
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
                    episode.animeAttachments.forEach { (imageType, url) ->
                        attachmentService.createAttachmentOrMarkAsActive(uuid!!, imageType, url = url, async = async)
                    }
                }
            }

        val animePlatform = animePlatformService.findByAnimePlatformAndId(anime, episode.platform, episode.animeId)

        if (animePlatform == null) {
            // Save the anime-platform association if it doesn't already exist
            animePlatformService.save(
                AnimePlatform(
                    anime = anime,
                    lastValidateDateTime = now,
                    platform = episode.platform,
                    platformId = episode.animeId
                )
            ).apply { traceActionService.createTraceAction(this, TraceAction.Action.CREATE) }
        } else {
            // Update the anime platform's last update date-time
            animePlatform.lastValidateDateTime = now
            animePlatformService.update(animePlatform)
            traceActionService.createTraceAction(animePlatform, TraceAction.Action.UPDATE)
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
                identifier = episode.getIdentifier(),
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

    /**
     * Retrieves or creates an `EpisodeMapping` for the given anime and episode.
     *
     * @param anime The `Anime` entity associated with the episode.
     * @param episode The `Episode` data from the platform containing details to map.
     * @param async A boolean indicating whether to perform certain operations asynchronously. Default is `true`.
     *
     * @return The `EpisodeMapping` entity corresponding to the given anime and episode.
     */
    private fun getEpisodeMapping(
        anime: Anime,
        episode: AbstractPlatform.Episode,
        async: Boolean = true
    ): EpisodeMapping {
        // Try to find an existing mapping or create a new one
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
                description = episode.description?.take(Constant.MAX_DESCRIPTION_LENGTH)
            )
        ).apply {
            // Create or activate a banner attachment for the episode if not in test mode
            if (!Constant.isTest) {
                attachmentService.createAttachmentOrMarkAsActive(uuid!!, ImageType.BANNER, url = episode.image, async = async)
            }
        }

        // Handle cases where the episode number is -1
        if (episode.number == -1) {
            // Determine the next available episode number
            val newNumber = episodeMappingService.findLastNumber(
                anime,
                episode.episodeType,
                episode.season,
                episode.platform,
                episode.audioLocale
            ) + 1

            // Check if a mapping with the new number already exists
            val existingMapping = episodeMappingService.findByAnimeSeasonEpisodeTypeNumber(
                anime.uuid,
                episode.season,
                episode.episodeType,
                newNumber
            )

            if (existingMapping != null) {
                // If the current mapping is unused, delete it
                if (findAllByMapping(mapping).isEmpty()) {
                    episodeMappingService.delete(mapping)
                }
                // Use the existing mapping
                mapping = existingMapping
            } else {
                // Update the current mapping with the new number
                mapping.number = newNumber
                episodeMappingService.update(mapping)
            }

            // Update the episode's number to match the mapping
            episode.number = mapping.number!!
        }

        return mapping
    }

    /**
     * Updates the `Anime` entity based on the provided episode and mapping data.
     *
     * @param anime The `Anime` entity to be updated.
     * @param episode The `Episode` data from the platform containing updated information.
     * @param mapping The `EpisodeMapping` associated with the episode.
     */
    private fun updateAnime(
        anime: Anime,
        episode: AbstractPlatform.Episode,
        mapping: EpisodeMapping
    ) {
        var needAnimeUpdate = false

        // Update the anime's last release date-time if the episode's release date-time is newer
        if (episode.releaseDateTime > anime.lastReleaseDateTime) {
            anime.lastReleaseDateTime = episode.releaseDateTime
            needAnimeUpdate = true
        }

        // Check if the episode's audio locale differs from the country's locale
        if (episode.audioLocale != episode.countryCode.locale) {
            // Determine the appropriate simulcast for the anime and mapping
            val simulcast = getSimulcast(anime, mapping)
            // Add the simulcast to the anime and check if it was successfully added
            val added = animeService.addSimulcastToAnime(anime, simulcast)
            needAnimeUpdate = needAnimeUpdate || added
        }

        // Update the anime entity in the database if any changes were made
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
            lastUpdateDateTime = Constant.oldLastUpdateDateTime,
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
