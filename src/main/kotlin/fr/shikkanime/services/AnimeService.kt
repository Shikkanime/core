package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Pageable
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.repositories.AnimeRepository
import io.ktor.http.*
import java.time.ZonedDateTime
import java.util.*

class AnimeService : AbstractService<Anime, AnimeRepository>() {
    @Inject
    private lateinit var animeRepository: AnimeRepository

    @Inject
    private lateinit var simulcastService: SimulcastService

    @Inject
    private lateinit var episodeService: EpisodeService

    override fun getRepository(): AnimeRepository {
        return animeRepository
    }

    fun findAll(sort: List<SortParameter>, page: Int, limit: Int): Pageable<Anime> {
        return animeRepository.findAll(sort, page, limit)
    }

    fun preIndex() {
        animeRepository.preIndex()
    }

    fun findByLikeName(countryCode: CountryCode, name: String?): List<Anime> {
        return animeRepository.findByLikeName(countryCode, name)
    }

    fun findByName(name: String?, countryCode: CountryCode, page: Int, limit: Int): Pageable<Anime> {
        return animeRepository.findByName(name, countryCode, page, limit)
    }

    fun findBySimulcast(
        uuid: UUID,
        countryCode: CountryCode,
        sort: List<SortParameter>,
        page: Int,
        limit: Int
    ): Pageable<Anime> {
        return animeRepository.findBySimulcast(uuid, countryCode, sort, page, limit)
    }

    override fun save(entity: Anime): Anime {
        entity.simulcasts = entity.simulcasts.map { simulcast ->
            simulcastService.findBySeasonAndYear(simulcast.season!!, simulcast.year!!) ?: simulcastService.save(
                simulcast
            )
        }.toMutableSet()

        val savedEntity = super.save(entity)
        ImageService.add(savedEntity.uuid!!, savedEntity.image!!, 480, 720)
        return savedEntity
    }

    fun update(uuid: UUID, parameters: Parameters): Anime? {
        val anime = find(uuid) ?: return null

        parameters["name"]?.takeIf { it.isNotBlank() }?.let { anime.name = it }
        parameters["releaseDateTime"]?.takeIf { it.isNotBlank() }
            ?.let { anime.releaseDateTime = ZonedDateTime.parse("$it:00Z") }

        parameters["image"]?.takeIf { it.isNotBlank() }?.let {
            anime.image = it
            ImageService.remove(anime.uuid!!)
            ImageService.add(anime.uuid, it, 480, 720)
        }

        parameters["description"]?.takeIf { it.isNotBlank() }?.let { anime.description = it }

        return super.update(anime)
    }

    override fun delete(entity: Anime) {
        episodeService.findByAnime(entity.uuid!!).forEach { episodeService.delete(it) }
        super.delete(entity)
    }
}