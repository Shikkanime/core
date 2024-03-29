package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.*
import fr.shikkanime.repositories.EpisodeRepository
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import io.ktor.http.*
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
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

    fun findAllUUIDAndImage() = episodeRepository.findAllUUIDAndImage()

    fun findAllByPlatformDeprecatedEpisodes(
        platform: Platform,
        lastUpdateDateTime: ZonedDateTime,
        notLike: String? = null
    ) =
        episodeRepository.findAllByPlatformDeprecatedEpisodes(platform, lastUpdateDateTime, notLike)

    fun findAllByDateRange(
        countryCode: CountryCode,
        start: ZonedDateTime,
        end: ZonedDateTime
    ) = episodeRepository.findAllByDateRange(countryCode, start, end)

    fun addImage(uuid: UUID, image: String, bypass: Boolean = false) {
        ImageService.add(uuid, ImageService.Type.IMAGE, image, 640, 360, bypass)
    }

    fun getSimulcast(anime: Anime, entity: Episode): Simulcast {
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
        val animeEpisodes = anime.uuid?.let { episodeRepository.findAllByAnime(it).sortedBy { episode -> episode.releaseDateTime } }
        val previousEpisode =
            animeEpisodes?.lastOrNull { it.releaseDateTime.isBefore(entity.releaseDateTime) && it.episodeType == entity.episodeType && it.langType == entity.langType }
        val diff = previousEpisode?.releaseDateTime?.until(entity.releaseDateTime, ChronoUnit.MONTHS) ?: -1

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

    fun addSimulcastToAnime(anime: Anime, simulcast: Simulcast) {
        if (anime.simulcasts.isEmpty() || anime.simulcasts.none { s -> s.uuid == simulcast.uuid }) {
            if (simulcast.uuid == null) {
                simulcastService.save(simulcast)
            }

            anime.simulcasts.add(simulcast)
        }
    }

    override fun save(entity: Episode): Episode {
        val copy = entity.anime!!.copy()
        val anime = animeService.findAllByLikeName(copy.countryCode!!, copy.name!!).firstOrNull() ?: animeService.save(copy)
        entity.anime = anime.copy()

        if (anime.banner.isNullOrBlank() && !copy.banner.isNullOrBlank()) {
            anime.banner = copy.banner
        }

        entity.number.takeIf { it == -1 }?.let {
            entity.number = episodeRepository.getLastNumber(
                anime.uuid!!,
                entity.platform!!,
                entity.season!!,
                entity.episodeType!!,
                entity.langType!!
            ) + 1
        }

        if (entity.langType == LangType.SUBTITLES && entity.episodeType != EpisodeType.FILM) {
            val simulcast = getSimulcast(anime, entity)
            addSimulcastToAnime(anime, simulcast)
        }

        if (anime.lastReleaseDateTime.isBefore(entity.releaseDateTime)) {
            anime.lastReleaseDateTime = entity.releaseDateTime
        }

        if (entity.anime != anime) {
            entity.anime = animeService.update(anime)
        }

        if (1000 < (entity.title?.length ?: 0)) {
            entity.title = entity.title!!.substring(0, 1000)
        }

        if (1000 < (entity.description?.length ?: 0)) {
            entity.description = entity.description!!.substring(0, 1000)
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
        parameters["description"]?.takeIf { it.isNotBlank() }?.let { episode.description = it }

        episode.lastUpdateDateTime = ZonedDateTime.now()
        val update = super.update(episode)
        MapCache.invalidate(Episode::class.java)
        return update
    }

    override fun delete(entity: Episode) {
        super.delete(entity)
        MapCache.invalidate(Episode::class.java)
    }
}