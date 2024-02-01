package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.*
import fr.shikkanime.repositories.EpisodeRepository
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import io.ktor.http.*
import org.hibernate.Hibernate
import java.time.ZonedDateTime
import java.util.*

class EpisodeService : AbstractService<Episode, EpisodeRepository>() {
    @Inject
    private lateinit var episodeRepository: EpisodeRepository

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var simulcastService: SimulcastService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    override fun getRepository() = episodeRepository

    fun findAllBy(
        countryCode: CountryCode?,
        anime: UUID?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int
    ) = episodeRepository.findAllBy(countryCode, anime, sort, page, limit)

    fun findAllHashes() = episodeRepository.findAllHashes()

    fun findAllByAnime(uuid: UUID) = episodeRepository.findAllByAnime(uuid)

    fun findByHash(hash: String?) = episodeRepository.findByHash(hash)

    fun findAllUUIDAndImage() = episodeRepository.findAllUUIDAndImage()

    fun findAllByPlatform(platform: Platform) = episodeRepository.findAllByPlatform(platform)

    fun addImage(uuid: UUID, image: String) {
        ImageService.add(uuid, ImageService.Type.IMAGE, image, 640, 360)
    }

    fun getSimulcast(entity: Episode): Simulcast {
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

        val isAnimeReleaseDateTimeBeforeMinusXDays = entity.anime!!.releaseDateTime.isBefore(adjustedDates[0])

        val choosenSimulcast = when {
            entity.number!! <= 1 && currentSimulcast != nextSimulcast -> nextSimulcast
            entity.number!! > 1 && isAnimeReleaseDateTimeBeforeMinusXDays && currentSimulcast != previousSimulcast -> previousSimulcast
            else -> currentSimulcast
        }

        return simulcastService.findBySeasonAndYear(choosenSimulcast.season!!, choosenSimulcast.year!!)
            ?: choosenSimulcast
    }

    override fun save(entity: Episode): Episode {
        val banner = entity.anime?.banner

        entity.anime = animeService.findAllByLikeName(entity.anime!!.countryCode!!, entity.anime!!.name!!).firstOrNull()
            ?: animeService.save(entity.anime!!)

        if (entity.anime?.banner.isNullOrBlank() && !banner.isNullOrBlank()) {
            entity.anime?.banner = banner
            animeService.update(entity.anime!!)
        }

        entity.number.takeIf { it == -1 }?.let {
            entity.number = episodeRepository.getLastNumber(
                entity.anime!!.uuid!!,
                entity.platform!!,
                entity.season!!,
                entity.episodeType!!,
                entity.langType!!
            ) + 1
        }

        if (entity.langType == LangType.SUBTITLES) {
            val simulcast = getSimulcast(entity)
            Hibernate.initialize(entity.anime!!.simulcasts)
            val animeSimulcasts = entity.anime!!.simulcasts

            if (animeSimulcasts.isEmpty() || animeSimulcasts.none { s -> s.uuid == simulcast.uuid }) {
                if (simulcast.uuid == null) {
                    simulcastService.save(simulcast)
                }

                animeSimulcasts.add(simulcast)
                animeService.update(entity.anime!!)
            }
        }

        val savedEntity = super.save(entity)
        addImage(savedEntity.uuid!!, savedEntity.image!!)
        MapCache.invalidate(Episode::class.java)
        return savedEntity
    }

    fun update(uuid: UUID, parameters: Parameters): Episode? {
        val episode = find(uuid) ?: return null

        parameters["episodeType"]?.let { episode.episodeType = EpisodeType.valueOf(it) }
        parameters["langType"]?.let { episode.langType = LangType.valueOf(it) }
        parameters["hash"]?.takeIf { it.isNotBlank() }?.let { episode.hash = it }
        parameters["releaseDateTime"]?.takeIf { it.isNotBlank() }
            ?.let { episode.releaseDateTime = ZonedDateTime.parse("$it:00Z") }
        parameters["season"]?.takeIf { it.isNotBlank() }?.let { episode.season = it.toInt() }
        parameters["number"]?.takeIf { it.isNotBlank() }?.let { episode.number = it.toInt() }
        parameters["title"]?.takeIf { it.isNotBlank() }?.let { episode.title = it }
        parameters["url"]?.takeIf { it.isNotBlank() }?.let { episode.url = it }

        parameters["image"]?.takeIf { it.isNotBlank() }?.let {
            episode.image = it
            ImageService.remove(episode.uuid!!, ImageService.Type.IMAGE)
            addImage(episode.uuid, it)
        }

        parameters["duration"]?.takeIf { it.isNotBlank() }?.let { episode.duration = it.toLong() }

        val update = super.update(episode)
        MapCache.invalidate(Episode::class.java)
        return update
    }

    override fun delete(entity: Episode) {
        super.delete(entity)
        MapCache.invalidate(Episode::class.java)
    }
}