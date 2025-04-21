package fr.shikkanime.services.admin

import com.google.inject.Inject
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.dtos.mappings.UpdateAllEpisodeMappingDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.*
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

/**
 * Service for managing episode mappings in an admin context.
 */
class EpisodeMappingAdminService {
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var attachmentService: AttachmentService

    /**
     * Merges an episode with an existing one and returns the updated existing episode.
     * All variants from the source episode are transferred to the target episode,
     * while preserving existing variants.
     */
    private fun mergeEpisodeMapping(
        sourceEpisode: EpisodeMapping,
        targetEpisode: EpisodeMapping
    ): EpisodeMapping? {
        // Get all variants of the target episode
        val targetVariants = episodeVariantService.findAllByMapping(targetEpisode)
        
        // Transfer variants from source episode to target episode, without duplication
        episodeVariantService.findAllByMapping(sourceEpisode).forEach { sourceVariant ->
            // Check if a similar variant already exists (same URL)
            val similarVariantExists = targetVariants.any { it.url == sourceVariant.url }
            
            if (!similarVariantExists) {
                // Transfer this variant
                sourceVariant.mapping = targetEpisode
                episodeVariantService.update(sourceVariant)
            }
        }

        // Remove references to the source episode
        memberFollowEpisodeService.findAllByEpisode(sourceEpisode).forEach { 
            memberFollowEpisodeService.delete(it) 
        }
        
        // Delete the source episode
        episodeMappingService.delete(sourceEpisode)

        // Update the target episode with the latest release date
        targetEpisode.apply {
            if (lastReleaseDateTime < sourceEpisode.lastReleaseDateTime) {
                lastReleaseDateTime = sourceEpisode.lastReleaseDateTime
            }
            lastUpdateDateTime = ZonedDateTime.now()
        }
        
        episodeMappingService.update(targetEpisode)
        traceActionService.createTraceAction(targetEpisode, TraceAction.Action.UPDATE)

        return episodeMappingService.find(targetEpisode.uuid!!)
    }

    /**
     * Checks if an episode can be merged with an existing one and performs the merge if possible.
     * Returns true if the episode was merged.
     */
    private fun hasBeenMerged(
        updateDto: UpdateAllEpisodeMappingDto,
        episode: EpisodeMapping,
        forcedUpdate: Boolean
    ): Boolean {
        if (updateDto.season != null || updateDto.episodeType != null) {
            val animeUuid = episode.anime?.uuid ?: return false
            val season = episode.season ?: return false
            val episodeType = episode.episodeType ?: return false
            val number = episode.number ?: return false
            
            episodeMappingService.findByAnimeSeasonEpisodeTypeNumber(animeUuid, season, episodeType, number)
                ?.takeIf { it.uuid != episode.uuid }
                ?.let { existingEpisode ->
                    mergeEpisodeMapping(episode, existingEpisode)?.apply {
                        lastUpdateDateTime = if (forcedUpdate) {
                            ZonedDateTime.parse("2000-01-01T00:00:00Z")
                        } else {
                            ZonedDateTime.now()
                        }
                        episodeMappingService.update(this)
                    }
                    return true
                }
        }
        return false
    }

    /**
     * Manages release dates for episode variants based on the provided configuration.
     * Returns the next date to use for sequencing if date incrementing is enabled.
     */
    private fun processReleaseDates(
        startDate: LocalDate?,
        episode: EpisodeMapping,
        updateDto: UpdateAllEpisodeMappingDto
    ): LocalDate? {
        val bindVoiceVariants = updateDto.bindVoiceVariants == true
        
        // If no start date but binding voice variants is enabled, synchronize variants
        if (startDate == null && bindVoiceVariants) {
            val variants = episodeVariantService.findAllByMapping(episode)
            variants.minOfOrNull { it.releaseDateTime }?.let { minDate ->
                variants.forEach { it.releaseDateTime = minDate }
                episodeVariantService.updateAll(variants)
            }
            return null
        }

        // Return early if no start date or no variants
        val currentDate = startDate ?: return null
        val variants = episodeVariantService.findAllByMapping(episode)
        if (variants.isEmpty()) {
            return currentDate
        }

        // Process variants based on configuration
        val getLangType = { audioLocale: String -> LangType.fromAudioLocale(episode.anime!!.countryCode!!, audioLocale) }
        
        val langTypes = variants.mapTo(HashSet()) { getLangType(it.audioLocale ?: "") }
        
        val variantsToUpdate = if (langTypes.size > 1 && !bindVoiceVariants) {
            // If multiple language types and not binding, only update subtitles
            variants.filter { variant -> 
                getLangType(variant.audioLocale ?: "") == LangType.SUBTITLES 
            }
        } else {
            variants
        }

        if (variantsToUpdate.isEmpty()) {
            return currentDate
        }

        // Update variant release dates
        val updated = if (!bindVoiceVariants) {
            // When not binding voice variants, update only the earliest variant
            variantsToUpdate.minByOrNull { it.releaseDateTime }?.let { variant ->
                variant.releaseDateTime = variant.releaseDateTime.with(currentDate)
                episodeVariantService.update(variant)
                true
            } == true
        } else {
            // When binding voice variants, update all variants
            variantsToUpdate.forEach { variant ->
                variant.releaseDateTime = variant.releaseDateTime.with(currentDate)
                episodeVariantService.update(variant)
            }
            true
        }

        // Calculate the next date if incrementing is enabled and variants were updated
        return if (updateDto.incrementDate == true && updated) {
            currentDate.plusWeeks(1)
        } else {
            currentDate
        }
    }

    /**
     * Updates multiple episodes based on the provided configuration.
     */
    fun updateAll(updateDto: UpdateAllEpisodeMappingDto) {
        val episodes = updateDto.uuids
            .mapNotNull { episodeMappingService.find(it) }
            .sortedWith(compareBy({ it.season }, { it.episodeType }, { it.number }))

        var currentDate = updateDto.startDate?.let { LocalDate.parse(it) }
        val counter = mutableMapOf<Pair<Int, EpisodeType>, Int>()
        val forcedUpdate = updateDto.forceUpdate == true
        val bindNumbers = updateDto.bindNumber == true

        episodes.forEach { episode ->
            // Handle episode numbering if binding is enabled
            if (bindNumbers) {
                episode.season?.let { season ->
                    episode.episodeType?.let { episodeType ->
                        val key = Pair(season, episodeType)
                        val value = counter.getOrDefault(key, 0) + 1
                        episode.number = value
                        counter[key] = value
                    }
                }
            }

            // Apply episode type and season updates
            updateDto.episodeType?.let { episode.episodeType = it }
            updateDto.season?.let { episode.season = it }

            // Process release dates
            currentDate = processReleaseDates(currentDate, episode, updateDto)
            
            // Skip if episode was merged
            if (hasBeenMerged(updateDto, episode, forcedUpdate)) {
                return@forEach
            }

            // Update the episode
            episode.lastUpdateDateTime = if (forcedUpdate) {
                ZonedDateTime.parse("2000-01-01T00:00:00Z") 
            } else {
                ZonedDateTime.now()
            }
            
            episodeMappingService.update(episode)
            traceActionService.createTraceAction(episode, TraceAction.Action.UPDATE)
        }

        // Recalculate simulcasts if dates were updated
        if (currentDate != null) {
            animeService.recalculateSimulcasts()
        }
    }

    /**
     * Updates date/time fields of an episode mapping if the DTO contains changes.
     */
    private fun updateEpisodeMappingDateTime(dto: EpisodeMappingDto, episode: EpisodeMapping) {
        if (dto.releaseDateTime.isNotBlank() && dto.releaseDateTime != episode.releaseDateTime.toString()) {
            episode.releaseDateTime = ZonedDateTime.parse(dto.releaseDateTime)
        }

        if (dto.lastReleaseDateTime.isNotBlank() && dto.lastReleaseDateTime != episode.lastReleaseDateTime.toString()) {
            episode.lastReleaseDateTime = ZonedDateTime.parse(dto.lastReleaseDateTime)
        }

        if (dto.lastUpdateDateTime.isNotBlank() && dto.lastUpdateDateTime != episode.lastUpdateDateTime.toString()) {
            episode.lastUpdateDateTime = ZonedDateTime.parse(dto.lastUpdateDateTime)
        }
    }

    /**
     * Updates episode variants from the provided DTO.
     * Only updates variants that are explicitly mentioned in the DTO, without removing others.
     */
    private fun updateEpisodeMappingVariants(
        dto: EpisodeMappingDto,
        targetEpisode: EpisodeMapping,
    ) {
        val variants = dto.variants ?: return
        if (variants.isEmpty()) return

        // Get only the variants mentioned in the DTO
        val variantsToModify = mutableListOf<EpisodeVariant>()
        
        variants.forEach { variantDto ->
            val variantUuid = variantDto.uuid
            val variant = episodeVariantService.find(variantUuid) ?: return@forEach
            
            variant.mapping = targetEpisode

            // Update variant fields if changed
            if (variantDto.releaseDateTime.isNotBlank() && variantDto.releaseDateTime != variant.releaseDateTime.toString()) {
                variant.releaseDateTime = ZonedDateTime.parse(variantDto.releaseDateTime)
            }

            if (variantDto.url.isNotBlank() && variantDto.url != variant.url.toString()) {
                variant.url = variantDto.url
            }

            if (variantDto.uncensored != variant.uncensored) {
                variant.uncensored = variantDto.uncensored
            }

            variantsToModify.add(variant)
        }
        
        // Update only the variants specified in the DTO
        if (variantsToModify.isNotEmpty()) {
            variantsToModify.forEach { episodeVariantService.update(it) }
        }
    }

    /**
     * Updates an episode mapping with the data from the provided DTO.
     * Returns the updated episode mapping or null if the episode wasn't found.
     */
    fun update(uuid: UUID, dto: EpisodeMappingDto): EpisodeMapping? {
        val episode = episodeMappingService.find(uuid) ?: return null
        val oldAnime = episode.anime!!

        // Handle anime change if needed
        if (handleAnimeChange(episode, oldAnime, dto)) {
            return null // Episode was merged or handling took care of the update
        }

        // Update the episode properties
        val episodeMapping = handleUpdateEpisodeProperties(episode, dto)

        if (episodeMapping != null) {
            return episodeMapping
        }
        
        // Handle episode variants
        val updatedEpisode = episodeMappingService.update(episode)
        updateEpisodeMappingVariants(dto, updatedEpisode)
        traceActionService.createTraceAction(episode, TraceAction.Action.UPDATE)
        
        return updatedEpisode
    }
    
    /**
     * Handles changes to the anime a mapping belongs to.
     * Returns true if the method handled the complete update process.
     */
    private fun handleAnimeChange(episode: EpisodeMapping, oldAnime: Anime, dto: EpisodeMappingDto): Boolean {
        if (dto.anime?.name.isNullOrBlank() || dto.anime!!.name == oldAnime.name) {
            return false
        }
        
        // Change anime
        val newAnime = animeService.findByName(oldAnime.countryCode!!, dto.anime!!.name)
            ?: throw IllegalArgumentException("Anime with name ${dto.anime!!.name} not found")
        
        // Check if a similar episode already exists in the new anime
        val existingEpisode = episodeMappingService.findByAnimeSeasonEpisodeTypeNumber(
            newAnime.uuid!!, dto.season, dto.episodeType, dto.number
        )?.takeIf { it.uuid != episode.uuid }
        
        if (existingEpisode != null) {
            // Keep the variants of the target episode
            val existingVariants = episodeVariantService.findAllByMapping(existingEpisode)
            
            // Transfer variants from source episode to target episode
            val sourceVariants = episodeVariantService.findAllByMapping(episode)
            val transferredVariants = mutableListOf<EpisodeVariant>()
            
            sourceVariants.forEach { sourceVariant ->
                // Check if a similar variant already exists (same URL)
                val similarVariantExists = existingVariants.any { it.url == sourceVariant.url }
                
                if (!similarVariantExists) {
                    // Transfer this variant
                    sourceVariant.mapping = existingEpisode
                    episodeVariantService.update(sourceVariant)
                    transferredVariants.add(sourceVariant)
                }
            }
            
            // Remove references to the source episode
            memberFollowEpisodeService.findAllByEpisode(episode).forEach { 
                memberFollowEpisodeService.delete(it) 
            }
            
            // Update dates if necessary
            if (existingEpisode.lastReleaseDateTime < episode.lastReleaseDateTime) {
                existingEpisode.lastReleaseDateTime = episode.lastReleaseDateTime
            }
            
            existingEpisode.lastUpdateDateTime = ZonedDateTime.now()
            val updatedEpisode = episodeMappingService.update(existingEpisode)

            // Delete the source episode
            episodeMappingService.delete(episode)
            
            // Create trace action for the target episode
            traceActionService.createTraceAction(existingEpisode, TraceAction.Action.UPDATE)
            
            // Update variants specified in the DTO
            updateEpisodeMappingVariants(dto, updatedEpisode)

            // Clean up old anime if needed
            cleanupAnimeIfEmpty(oldAnime, updatedEpisode.uuid)
            return true
        }
        
        // If no merge, simply change the anime
        episode.anime = newAnime
        episodeMappingService.update(episode)
        
        // Clean up old anime if necessary
        cleanupAnimeIfEmpty(oldAnime, episode.uuid)
        return false
    }
    
    /**
     * Updates the episode properties from the DTO.
     */
    private fun handleUpdateEpisodeProperties(episode: EpisodeMapping, dto: EpisodeMappingDto): EpisodeMapping? {
        // Update date/time fields
        updateEpisodeMappingDateTime(dto, episode)
        
        // Handle episode type/season/number changes
        if (dto.episodeType != episode.episodeType || dto.season != episode.season || dto.number != episode.number) {
            // Check for existing episode with the new properties
            val existingEpisode = episodeMappingService.findByAnimeSeasonEpisodeTypeNumber(
                episode.anime!!.uuid!!, dto.season, dto.episodeType, dto.number
            )
            
            if (existingEpisode != null) {
                return mergeEpisodeMapping(episode, existingEpisode)
            }
            
            // Update episode properties
            episode.episodeType = dto.episodeType
            episode.season = dto.season
            episode.number = dto.number
        }
        
        // Update other metadata
        updateEpisodeMetadata(episode, dto)
        
        // Update timestamps
        episode.lastUpdateDateTime = ZonedDateTime.now()
        return null
    }
    
    /**
     * Updates metadata fields like title, description, duration, and image.
     */
    private fun updateEpisodeMetadata(episode: EpisodeMapping, dto: EpisodeMappingDto) {
        // Update title if changed
        if (!dto.title.isNullOrBlank() && dto.title != episode.title) {
            episode.title = dto.title
        }
        
        // Update description if changed
        if (!dto.description.isNullOrBlank() && dto.description != episode.description) {
            episode.description = dto.description
        }
        
        // Update duration if changed
        if (dto.duration != episode.duration) {
            episode.duration = dto.duration
        }
        
        // Update image if provided
        if (!dto.image.isNullOrBlank()) {
            attachmentService.createAttachmentOrMarkAsActive(
                episode.uuid!!, ImageType.BANNER, url = dto.image
            )
        }
    }

    /**
     * Deletes an anime if it has no remaining episodes.
     */
    private fun cleanupAnimeIfEmpty(anime: Anime, excludeEpisodeUuid: UUID?) {
        val remainingEpisodes = episodeMappingService.findAllByAnime(anime).toMutableList()
        excludeEpisodeUuid?.let { uuid -> remainingEpisodes.removeIf { it.uuid == uuid } }
        
        if (remainingEpisodes.isEmpty()) {
            animeService.delete(anime)
        }
    }
}