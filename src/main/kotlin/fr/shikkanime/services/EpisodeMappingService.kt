package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.EpisodeMappingDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.repositories.EpisodeMappingRepository
import fr.shikkanime.repositories.EpisodeVariantRepository
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import java.time.ZonedDateTime
import java.util.*

class EpisodeMappingService : AbstractService<EpisodeMapping, EpisodeMappingRepository>() {
    @Inject
    private lateinit var episodeMappingRepository: EpisodeMappingRepository

    @Inject
    private lateinit var episodeVariantRepository: EpisodeVariantRepository

    @Inject
    private lateinit var animeService: AnimeService

    override fun getRepository() = episodeMappingRepository

    fun findAllBy(
        countryCode: CountryCode?,
        anime: Anime?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        status: Status? = null
    ) = episodeMappingRepository.findAllBy(countryCode, anime, sort, page, limit, status)

    fun findAllUuidAndImage() = episodeMappingRepository.findAllUuidAndImage()

    fun findLastNumber(anime: Anime, episodeType: EpisodeType, season: Int, platform: Platform, audioLocale: String) =
        episodeMappingRepository.findLastNumber(anime, episodeType, season, platform, audioLocale)

    fun findByAnimeEpisodeTypeSeasonNumber(anime: Anime, episodeType: EpisodeType, season: Int, number: Int) =
        episodeMappingRepository.findByAnimeEpisodeTypeSeasonNumber(anime, episodeType, season, number)

    fun addImage(uuid: UUID, image: String, bypass: Boolean = false) {
        ImageService.add(uuid, ImageService.Type.IMAGE, image, 640, 360, bypass)
    }

    override fun save(entity: EpisodeMapping): EpisodeMapping {
        val save = super.save(entity)
        addImage(save.uuid!!, save.image!!)
        MapCache.invalidate(EpisodeMapping::class.java)
        return save
    }

    fun update(uuid: UUID, entity: EpisodeMappingDto): EpisodeMapping? {
        val episode = find(uuid) ?: return null

        updateEpisodeMappingAnime(entity, episode)
        updateEpisodeMappingDateTime(entity, episode)

        if (!(entity.episodeType == episode.episodeType && entity.season == episode.season && entity.number == episode.number)) {
            // Find if the episode already exists
            val existing =
                findByAnimeEpisodeTypeSeasonNumber(episode.anime!!, entity.episodeType, entity.season, entity.number)

            if (existing != null) {
                // Set the variants of the current episode to the existing episode
                episode.variants.forEach { variant ->
                    variant.mapping = existing
                    episodeVariantRepository.update(variant)
                }

                // If the episode already exists, we delete the current episode
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
        var update = super.update(episode)

        update = updateEpisodeMappingVariants(entity, episode, update, uuid)
        MapCache.invalidate(EpisodeMapping::class.java)
        return update
    }

    private fun updateEpisodeMappingAnime(entity: EpisodeMappingDto, episode: EpisodeMapping) {
        if (entity.anime.name.isNotBlank() && entity.anime.name != episode.anime!!.name) {
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

            if (oldAnime.mappings.isEmpty()) {
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
        uuid: UUID
    ): EpisodeMapping {
        var update1 = update
        if (!entity.variants.isNullOrEmpty()) {
            val oldList = mutableSetOf(*episode.variants.toTypedArray())
            episode.variants.clear()

            entity.variants.forEach { variantDto ->
                val variant = episodeVariantRepository.find(variantDto.uuid) ?: return@forEach
                episode.variants.add(variant)
                oldList.removeIf { it.uuid == variantDto.uuid }
            }

            oldList.forEach { episodeVariantRepository.delete(it) }
            MapCache.invalidate(EpisodeVariant::class.java)
            update1 = find(uuid)!!
        }
        return update1
    }

    override fun delete(entity: EpisodeMapping) {
        entity.variants.forEach { episodeVariantRepository.delete(it) }
        super.delete(entity)
        MapCache.invalidate(EpisodeMapping::class.java, EpisodeVariant::class.java)
    }
}