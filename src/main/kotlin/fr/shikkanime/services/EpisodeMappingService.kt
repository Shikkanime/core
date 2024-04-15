package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.EpisodeMappingDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.*
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

    fun save(entity: Episode): EpisodeMapping {
        val episodeMapping = findByAnimeEpisodeTypeSeasonNumber(
            entity.anime!!,
            entity.episodeType!!,
            entity.season!!,
            entity.number!!
        ) ?: createEpisodeMapping(entity)

        val audioLocale = (if (entity.platform == Platform.CRUN) entity.audioLocale else null) ?: "ja-JP"
        val id = ".{2}-.{4}-(.*)-.*".toRegex().find(entity.hash!!)!!.groupValues[1]
        val uncensored = entity.image!!.contains("nc/", true)
        val identifier = StringUtils.getIdentifier(entity.anime!!.countryCode!!, entity.platform!!, id, audioLocale, uncensored)

        if (episodeMapping.variants.none { it.identifier == identifier }) {
            episodeVariantRepository.save(getVariant(episodeMapping, entity, audioLocale, identifier, uncensored))
            episodeMapping.lastReleaseDateTime = entity.releaseDateTime
            episodeMappingRepository.update(episodeMapping)
        }

        return episodeMapping
    }

    private fun createEpisodeMapping(entity: Episode) = episodeMappingRepository.save(
        EpisodeMapping(
            anime = entity.anime!!,
            releaseDateTime = entity.releaseDateTime,
            lastReleaseDateTime = entity.releaseDateTime,
            lastUpdateDateTime = entity.releaseDateTime,
            episodeType = entity.episodeType!!,
            season = entity.season!!,
            number = entity.number!!,
            duration = entity.duration,
            title = entity.title,
            description = entity.description,
            image = entity.image,
        ).apply {
            status = StringUtils.getStatus(this)
        }
    )

    private fun getVariant(
        episodeMapping: EpisodeMapping,
        entity: Episode,
        audioLocale: String,
        identifier: String,
        uncensored: Boolean,
    ) = EpisodeVariant(
        mapping = episodeMapping,
        releaseDateTime = entity.releaseDateTime,
        platform = entity.platform,
        audioLocale = audioLocale,
        identifier = identifier,
        url = entity.url,
        uncensored = uncensored,
    )

    fun update(uuid: UUID, entity: EpisodeMappingDto): EpisodeMapping? {
        val episode = find(uuid) ?: return null

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

        if (entity.releaseDateTime.isNotBlank() && entity.releaseDateTime != episode.releaseDateTime.toString()) {
            episode.releaseDateTime = ZonedDateTime.parse(entity.releaseDateTime)
        }

        if (entity.lastReleaseDateTime.isNotBlank() && entity.lastReleaseDateTime != episode.lastReleaseDateTime.toString()) {
            episode.lastReleaseDateTime = ZonedDateTime.parse(entity.lastReleaseDateTime)
        }

        if (entity.lastUpdateDateTime.isNotBlank() && entity.lastUpdateDateTime != episode.lastUpdateDateTime.toString()) {
            episode.lastUpdateDateTime = ZonedDateTime.parse(entity.lastUpdateDateTime)
        }

        if (entity.episodeType != episode.episodeType) {
            episode.episodeType = entity.episodeType
        }

        if (entity.season != episode.season) {
            episode.season = entity.season
        }

        if (entity.number != episode.number) {
            episode.number = entity.number
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
        MapCache.invalidate(EpisodeMapping::class.java)
        return update
    }

    override fun delete(entity: EpisodeMapping) {
        entity.variants.forEach { episodeVariantRepository.delete(it) }
        super.delete(entity)
        MapCache.invalidate(EpisodeMapping::class.java, EpisodeVariant::class.java)
    }
}