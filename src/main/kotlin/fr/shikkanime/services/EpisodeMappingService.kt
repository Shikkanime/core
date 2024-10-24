package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.repositories.EpisodeMappingRepository
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
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

    fun findAllBy(
        countryCode: CountryCode?,
        anime: Anime?,
        season: Int?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        status: Status? = null
    ) = episodeMappingRepository.findAllBy(countryCode, anime, season, sort, page, limit, status)

    fun findAllUuidAndImage() = episodeMappingRepository.findAllUuidAndImage()

    fun findAllByAnime(anime: Anime) = episodeMappingRepository.findAllByAnime(anime)

    fun findAllNeedUpdateByPlatform(platform: Platform, lastDateTime: ZonedDateTime) =
        episodeMappingRepository.findAllNeedUpdateByPlatform(platform, lastDateTime)

    fun findAllSeo() = episodeMappingRepository.findAllSeo()

    fun findLastNumber(anime: Anime, episodeType: EpisodeType, season: Int, platform: Platform, audioLocale: String) =
        episodeMappingRepository.findLastNumber(anime, episodeType, season, platform, audioLocale)

    fun findByAnimeSeasonEpisodeTypeNumber(animeUuid: UUID, season: Int, episodeType: EpisodeType, number: Int) =
        episodeMappingRepository.findByAnimeSeasonEpisodeTypeNumber(animeUuid, season, episodeType, number)

    fun findPreviousEpisode(episode: EpisodeMapping) = episodeMappingRepository.findPreviousEpisode(episode)

    fun findNextEpisode(episode: EpisodeMapping) = episodeMappingRepository.findNextEpisode(episode)

    fun findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime: Anime, episode: EpisodeMapping) =
        episodeMappingRepository.findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime, episode)

    fun updateAllReleaseDate() = episodeMappingRepository.updateAllReleaseDate()

    fun addImage(uuid: UUID, image: String, bypass: Boolean = false) {
        ImageService.add(uuid, ImageService.Type.IMAGE, image, 640, 360, bypass)
    }

    override fun save(entity: EpisodeMapping): EpisodeMapping {
        val save = super.save(entity)
        addImage(save.uuid!!, save.image!!)
        MapCache.invalidate(EpisodeMapping::class.java)
        traceActionService.createTraceAction(entity, TraceAction.Action.CREATE)
        return save
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
                // Set the variants of the current episode to the existing episode
                episodeVariantService.findAllByMapping(episode).forEach { variant ->
                    variant.mapping = existing
                    episodeVariantService.update(variant)
                }

                // If the episode already exists, we delete the current episode
                memberFollowEpisodeService.findAllByEpisode(episode).forEach { memberFollowEpisodeService.delete(it) }
                super.delete(episode)

                if (existing.lastReleaseDateTime.isBefore(episode.lastReleaseDateTime)) {
                    existing.lastReleaseDateTime = episode.lastReleaseDateTime
                }

                existing.lastUpdateDateTime = ZonedDateTime.now()
                update(existing)
                MapCache.invalidate(EpisodeMapping::class.java, EpisodeVariant::class.java)
                return find(existing.uuid!!)
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
            ImageService.remove(episode.uuid!!, ImageService.Type.IMAGE)
            addImage(episode.uuid, episode.image!!)
        }

        if (entity.duration != episode.duration) {
            episode.duration = entity.duration
        }

        episode.status = StringUtils.getStatus(episode)
        episode.lastUpdateDateTime = ZonedDateTime.now()
        val update = super.update(episode)
        updateEpisodeMappingVariants(entity, episode, update)
        MapCache.invalidate(EpisodeMapping::class.java)
        traceActionService.createTraceAction(episode, TraceAction.Action.UPDATE)
        return update
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
                MapCache.invalidate(Anime::class.java)
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
        MapCache.invalidate(EpisodeVariant::class.java)
    }

    override fun delete(entity: EpisodeMapping) {
        episodeVariantService.findAllByMapping(entity).forEach { episodeVariantService.delete(it) }
        memberFollowEpisodeService.findAllByEpisode(entity).forEach { memberFollowEpisodeService.delete(it) }
        super.delete(entity)
        MapCache.invalidate(EpisodeMapping::class.java, EpisodeVariant::class.java)
        traceActionService.createTraceAction(entity, TraceAction.Action.DELETE)
    }
}