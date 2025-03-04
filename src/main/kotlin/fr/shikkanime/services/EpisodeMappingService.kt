package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.dtos.mappings.UpdateAllEpisodeMappingDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.repositories.EpisodeMappingRepository
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.StringUtils
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class EpisodeMappingService : AbstractService<EpisodeMapping, EpisodeMappingRepository>() {
    @Inject
    private lateinit var episodeMappingRepository: EpisodeMappingRepository

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    @Inject
    private lateinit var traceActionService: TraceActionService

    override fun getRepository() = episodeMappingRepository

    fun findAllUuids() = episodeMappingRepository.findAllUuids()

    fun findAllBy(
        countryCode: CountryCode?,
        anime: Anime?,
        season: Int?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        status: Status? = null
    ) = episodeMappingRepository.findAllBy(countryCode, anime, season, sort, page, limit, status)

    fun findAllAnimeUuidImageBannerAndUuidImage() = episodeMappingRepository.findAllAnimeUuidImageBannerAndUuidImage()

    fun findAllByAnime(anime: Anime) = episodeMappingRepository.findAllByAnime(anime.uuid!!)

    fun findAllByAnime(animeUuid: UUID) = episodeMappingRepository.findAllByAnime(animeUuid)

    fun findAllNeedUpdateByPlatforms(platforms: List<Platform>, lastDateTime: ZonedDateTime) =
        episodeMappingRepository.findAllNeedUpdateByPlatforms(platforms, lastDateTime)

    fun findAllSeo() = episodeMappingRepository.findAllSeo()

    fun findAllSimulcasted(ignoreEpisodeTypes: Set<EpisodeType>, ignoreAudioLocale: String) =
        episodeMappingRepository.findAllSimulcasted(ignoreEpisodeTypes, ignoreAudioLocale)

    fun findLastNumber(anime: Anime, episodeType: EpisodeType, season: Int, platform: Platform, audioLocale: String) =
        episodeMappingRepository.findLastNumber(anime, episodeType, season, platform, audioLocale)

    fun findByAnimeSeasonEpisodeTypeNumber(animeUuid: UUID, season: Int, episodeType: EpisodeType, number: Int) =
        episodeMappingRepository.findByAnimeSeasonEpisodeTypeNumber(animeUuid, season, episodeType, number)

    fun findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime: Anime, episode: EpisodeMapping) =
        episodeMappingRepository.findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime, episode)

    fun findMinimalReleaseDateTime() = episodeMappingRepository.findMinimalReleaseDateTime()

    fun updateAllReleaseDate() = episodeMappingRepository.updateAllReleaseDate()

    fun addImage(uuid: UUID, image: String, bypass: Boolean = false) {
        ImageService.add(uuid, ImageService.Type.IMAGE, image, 640, 360, bypass)
    }

    override fun save(entity: EpisodeMapping): EpisodeMapping {
        val save = super.save(entity)
        if (!Constant.disableImageConversion) addImage(save.uuid!!, save.image!!)
        traceActionService.createTraceAction(entity, TraceAction.Action.CREATE)
        return save
    }

    fun updateAll(updateAllEpisodeMappingDto: UpdateAllEpisodeMappingDto) {
        val episodes = updateAllEpisodeMappingDto.uuids
            .mapNotNull { find(it) }
            .sortedWith(compareBy({ it.season }, { it.episodeType }, { it.number }))

        var startDate = updateAllEpisodeMappingDto.startDate?.let { LocalDate.parse(it) }
        val counter = mutableMapOf<Pair<Int, EpisodeType>, Int>()

        episodes.forEach { episode ->
            if (updateAllEpisodeMappingDto.bindNumber == true) {
                val key = Pair(episode.season!!, episode.episodeType!!)
                val value = counter.getOrDefault(key, 0) + 1
                episode.number = value
                counter[key] = value
            }

            val forcedUpdate = updateAllEpisodeMappingDto.forceUpdate == true

            updateAllEpisodeMappingDto.episodeType?.let { episode.episodeType = it }
            updateAllEpisodeMappingDto.season?.let { episode.season = it }

            startDate = incrementReleaseDate(startDate, episode, updateAllEpisodeMappingDto)
            if (hasBeenMerged(updateAllEpisodeMappingDto, episode, forcedUpdate)) return@forEach

            episode.status = StringUtils.getStatus(episode)
            episode.lastUpdateDateTime = if (forcedUpdate) ZonedDateTime.parse("2000-01-01T00:00:00Z") else ZonedDateTime.now()
            super.update(episode)
            traceActionService.createTraceAction(episode, TraceAction.Action.UPDATE)
        }

        if (startDate != null) {
            animeService.recalculateSimulcasts()
        }
    }

    private fun hasBeenMerged(
        updateAllEpisodeMappingDto: UpdateAllEpisodeMappingDto,
        episode: EpisodeMapping,
        forcedUpdate: Boolean
    ): Boolean {
        if (updateAllEpisodeMappingDto.season != null || updateAllEpisodeMappingDto.episodeType != null) {
            findByAnimeSeasonEpisodeTypeNumber(episode.anime!!.uuid!!, episode.season!!, episode.episodeType!!, episode.number!!)
                ?.takeIf { it.uuid != episode.uuid }
                ?.let { existing ->
                    mergeEpisodeMapping(episode, existing)?.apply {
                        if (forcedUpdate) lastUpdateDateTime = ZonedDateTime.parse("2000-01-01T00:00:00Z") else ZonedDateTime.now()
                        super.update(this)
                    }

                    return true
                }
        }

        return false
    }

    private fun incrementReleaseDate(
        startDate: LocalDate?,
        episode: EpisodeMapping,
        updateAllEpisodeMappingDto: UpdateAllEpisodeMappingDto
    ): LocalDate? {
        if (startDate == null && updateAllEpisodeMappingDto.bindVoiceVariants == true) {
            val variants = episodeVariantService.findAllByMapping(episode)
            val date = variants.minOfOrNull { it.releaseDateTime } ?: return null
            variants.forEach { it.releaseDateTime = date }
            episodeVariantService.updateAll(variants)
            return null
        }

        var tmpDate = startDate ?: return null
        val variants = episodeVariantService.findAllByMapping(episode)

        if (variants.isEmpty()) {
            return tmpDate
        }

        val getLangType = { audioLocale: String -> LangType.fromAudioLocale(episode.anime!!.countryCode!!, audioLocale) }
        val langTypes = variants.mapTo(HashSet()) { getLangType(it.audioLocale!!) }

        val filteredVariants = if (langTypes.size > 1 && updateAllEpisodeMappingDto.bindVoiceVariants != true) {
            variants.filter { getLangType(it.audioLocale!!) == LangType.SUBTITLES }
        } else {
            variants
        }

        if (filteredVariants.isEmpty()) {
            return tmpDate
        }

        val needIncrement = if (updateAllEpisodeMappingDto.bindVoiceVariants != true) {
            filteredVariants.minByOrNull { it.releaseDateTime }?.let { originalVariant ->
                originalVariant.releaseDateTime = originalVariant.releaseDateTime.with(tmpDate)
                episodeVariantService.update(originalVariant)
                true
            } == true
        } else {
            filteredVariants.forEach { variant ->
                variant.releaseDateTime = variant.releaseDateTime.with(tmpDate)
                episodeVariantService.update(variant)
            }

            true
        }

        return if (updateAllEpisodeMappingDto.incrementDate == true && needIncrement) {
            tmpDate.plusWeeks(1)
        } else {
            tmpDate
        }
    }

    fun update(uuid: UUID, entity: EpisodeMappingDto): EpisodeMapping? {
        val episode = find(uuid) ?: return null

        updateEpisodeMappingAnime(entity, episode)
        updateEpisodeMappingDateTime(entity, episode)

        if (!(entity.episodeType == episode.episodeType && entity.season == episode.season && entity.number == episode.number)) {
            // Find if the episode already exists
            val existing =
                findByAnimeSeasonEpisodeTypeNumber(episode.anime!!.uuid!!, entity.season, entity.episodeType, entity.number)

            if (existing != null) {
                return mergeEpisodeMapping(episode, existing)
            } else {
                episode.episodeType = entity.episodeType
                episode.season = entity.season
                episode.number = entity.number
            }
        }

        if (entity.title?.isNotBlank() == true && entity.title != episode.title) {
            episode.title = entity.title
        }

        if (entity.description?.isNotBlank() == true && entity.description != episode.description) {
            episode.description = entity.description
        }

        if (entity.image.isNotBlank() && entity.image != episode.image) {
            episode.image = entity.image
            addImage(episode.uuid!!, episode.image!!, true)
        }

        if (entity.duration != episode.duration) {
            episode.duration = entity.duration
        }

        episode.status = StringUtils.getStatus(episode)
        episode.lastUpdateDateTime = ZonedDateTime.now()
        val update = super.update(episode)
        updateEpisodeMappingVariants(entity, episode, update)
        traceActionService.createTraceAction(episode, TraceAction.Action.UPDATE)
        return update
    }

    private fun mergeEpisodeMapping(
        episode: EpisodeMapping,
        existing: EpisodeMapping
    ): EpisodeMapping? {
        // Set the variants of the current episode to the existing episode
        episodeVariantService.findAllByMapping(episode).forEach { variant ->
            variant.mapping = existing
            episodeVariantService.update(variant)
        }

        // If the episode already exists, we delete the current episode
        memberFollowEpisodeService.findAllByEpisode(episode).forEach { memberFollowEpisodeService.delete(it) }
        traceActionService.createTraceAction(episode, TraceAction.Action.DELETE)
        super.delete(episode)

        if (existing.lastReleaseDateTime < episode.lastReleaseDateTime) {
            existing.lastReleaseDateTime = episode.lastReleaseDateTime
        }

        existing.lastUpdateDateTime = ZonedDateTime.now()
        update(existing)
        traceActionService.createTraceAction(existing, TraceAction.Action.UPDATE)

        return find(existing.uuid!!)
    }

    private fun updateEpisodeMappingAnime(entity: EpisodeMappingDto, episode: EpisodeMapping) {
        if (entity.anime.name.isNotBlank() && entity.anime.name != episode.anime?.name) {
            val oldAnimeId = episode.anime!!.uuid!!
            val findByName = requireNotNull(
                animeService.findByName(
                    episode.anime!!.countryCode!!,
                    entity.anime.name
                )
            ) { "Anime with name ${entity.anime.name} not found" }
            episode.anime = findByName
            update(episode)

            if (episode.title.isNullOrBlank()) {
                episode.title = findByName.name
            }

            val oldAnime = animeService.find(oldAnimeId)!!

            if (findAllByAnime(oldAnime).isEmpty()) {
                animeService.delete(oldAnime)
            }
        }
    }

    private fun updateEpisodeMappingDateTime(entity: EpisodeMappingDto, episode: EpisodeMapping) {
        if (entity.releaseDateTime.isNotBlank() && entity.releaseDateTime != episode.releaseDateTime.toString()) {
            episode.releaseDateTime = ZonedDateTime.parse(entity.releaseDateTime)
        }

        if (entity.lastReleaseDateTime.isNotBlank() && entity.lastReleaseDateTime != episode.lastReleaseDateTime.toString()) {
            episode.lastReleaseDateTime = ZonedDateTime.parse(entity.lastReleaseDateTime)
        }

        if (entity.lastUpdateDateTime.isNotBlank() && entity.lastUpdateDateTime != episode.lastUpdateDateTime.toString()) {
            episode.lastUpdateDateTime = ZonedDateTime.parse(entity.lastUpdateDateTime)
        }
    }

    private fun updateEpisodeMappingVariants(
        entity: EpisodeMappingDto,
        episode: EpisodeMapping,
        update: EpisodeMapping,
    ) {
        if (entity.variants.isNullOrEmpty()) {
            return
        }

        val oldList = mutableSetOf(*episodeVariantService.findAllByMapping(episode).toTypedArray())

        entity.variants.forEach { variantDto ->
            val variant = episodeVariantService.find(variantDto.uuid) ?: return@forEach
            variant.mapping = update

            if (variantDto.releaseDateTime.isNotBlank() && variantDto.releaseDateTime != variant.releaseDateTime.toString()) {
                variant.releaseDateTime = ZonedDateTime.parse(variantDto.releaseDateTime)
            }

            if (variantDto.url.isNotBlank() && variantDto.url != variant.url.toString()) {
                variant.url = variantDto.url
            }

            if (variantDto.uncensored != variant.uncensored) {
                variant.uncensored = variantDto.uncensored
            }

            oldList.removeIf { it.uuid == variantDto.uuid }
            episodeVariantService.update(variant)
        }

        oldList.forEach { episodeVariantService.delete(it) }
    }

    override fun delete(entity: EpisodeMapping) {
        episodeVariantService.findAllByMapping(entity).forEach { episodeVariantService.delete(it) }
        memberFollowEpisodeService.findAllByEpisode(entity).forEach { memberFollowEpisodeService.delete(it) }
        super.delete(entity)
        traceActionService.createTraceAction(entity, TraceAction.Action.DELETE)
    }
}