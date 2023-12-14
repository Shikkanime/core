package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.repositories.AnimeRepository

class AnimeService : AbstractService<Anime, AnimeRepository>() {
    @Inject
    private lateinit var animeRepository: AnimeRepository

    @Inject
    private lateinit var simulcastService: SimulcastService

    override fun getRepository(): AnimeRepository {
        return animeRepository
    }

    fun preIndex() {
        animeRepository.preIndex()
    }

    fun findByLikeName(countryCode: CountryCode, name: String?): List<Anime> {
        return animeRepository.findByLikeName(countryCode, name)
    }

    fun findByName(countryCode: CountryCode, name: String?): List<Anime> {
        return animeRepository.findByName(countryCode, name)
    }

    override fun save(entity: Anime): Anime {
        entity.simulcasts = entity.simulcasts.map { simulcast ->
            simulcastService.findBySeasonAndYear(simulcast.season!!, simulcast.year!!) ?: simulcastService.save(simulcast)
        }.toMutableSet()

        val savedEntity = super.save(entity)
        ImageService.add(savedEntity.uuid!!, savedEntity.image!!, 480, 720)
        return savedEntity
    }
}