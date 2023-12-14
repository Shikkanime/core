package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.repositories.EpisodeRepository
import org.hibernate.Hibernate

class EpisodeService : AbstractService<Episode, EpisodeRepository>() {
    private val simulcastRange = 10
    private val seasons = listOf("WINTER", "SPRING", "SUMMER", "AUTUMN")

    @Inject
    private lateinit var episodeRepository: EpisodeRepository

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var simulcastService: SimulcastService

    override fun getRepository(): EpisodeRepository {
        return episodeRepository
    }

    fun findAllHashes(): List<String> {
        return episodeRepository.findAllHashes()
    }

    fun findByHash(hash: String?): Episode? {
        return episodeRepository.findByHash(hash)
    }

    override fun save(entity: Episode): Episode {
        entity.anime = animeService.findByLikeName(entity.anime!!.countryCode!!, entity.anime!!.name!!).firstOrNull() ?: animeService.save(entity.anime!!)

        if (entity.langType == LangType.SUBTITLES) {
            val adjustedDates = listOf(-simulcastRange, 0, simulcastRange).map { days ->
                entity.releaseDateTime.plusDays(days.toLong())
            }

            val simulcasts = adjustedDates.map {
                Simulcast(season = seasons[(it.monthValue - 1) / 3], year = it.year)
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

            val simulcast = simulcastService.findBySeasonAndYear(choosenSimulcast.season!!, choosenSimulcast.year!!) ?: choosenSimulcast

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
}