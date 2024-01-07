package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.Pageable
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.repositories.EpisodeRepository
import fr.shikkanime.utils.Constant
import io.ktor.http.*
import org.hibernate.Hibernate
import java.time.ZonedDateTime
import java.util.*

class EpisodeService : AbstractService<Episode, EpisodeRepository>() {
    private val simulcastRange = 10

    @Inject
    private lateinit var episodeRepository: EpisodeRepository

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var simulcastService: SimulcastService

    override fun getRepository(): EpisodeRepository {
        return episodeRepository
    }

    fun findAllBy(
        countryCode: CountryCode?,
        anime: UUID?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int
    ): Pageable<Episode> {
        return episodeRepository.findAllBy(countryCode, anime, sort, page, limit)
    }

    fun findAllHashes(): List<String> {
        return episodeRepository.findAllHashes()
    }

    fun findByHash(hash: String?): Episode? {
        return episodeRepository.findByHash(hash)
    }

    fun findByAnime(uuid: UUID): List<Episode> {
        return episodeRepository.findByAnime(uuid)
    }

    override fun save(entity: Episode): Episode {
        entity.anime = animeService.findByLikeName(entity.anime!!.countryCode!!, entity.anime!!.name!!).firstOrNull()
            ?: animeService.save(entity.anime!!)

        if (entity.langType == LangType.SUBTITLES) {
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

            val simulcast = simulcastService.findBySeasonAndYear(choosenSimulcast.season!!, choosenSimulcast.year!!)
                ?: choosenSimulcast

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
        ImageService.add(savedEntity.uuid!!, savedEntity.image!!, 640, 360)
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
            ImageService.remove(episode.uuid!!)
            ImageService.add(episode.uuid, episode.image!!, 640, 360)
        }

        parameters["duration"]?.takeIf { it.isNotBlank() }?.let { episode.duration = it.toLong() }

        return super.update(episode)
    }
}