package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
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

    override fun getRepository() = animeRepository

    fun findAllBy(
        countryCode: CountryCode?,
        simulcast: UUID?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int
    ) = animeRepository.findAllBy(countryCode, simulcast, sort, page, limit)

    fun preIndex() = animeRepository.preIndex()

    fun findAllByLikeName(countryCode: CountryCode, name: String?) =
        animeRepository.findAllByLikeName(countryCode, name)

    fun findAllByName(name: String, countryCode: CountryCode?, page: Int, limit: Int) =
        animeRepository.findAllByName(name, countryCode, page, limit)

    fun addImage(anime: Anime) {
        ImageService.add(anime.uuid!!, anime.image!!, 480, 720)
    }

    override fun save(entity: Anime): Anime {
        entity.simulcasts = entity.simulcasts.map { simulcast ->
            simulcastService.findBySeasonAndYear(simulcast.season!!, simulcast.year!!) ?: simulcastService.save(
                simulcast
            )
        }.toMutableSet()

        val savedEntity = super.save(entity)
        addImage(savedEntity)
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
            addImage(anime)
        }

        parameters["description"]?.takeIf { it.isNotBlank() }?.let { anime.description = it }

        return super.update(anime)
    }

    override fun delete(entity: Anime) {
        episodeService.findAllByAnime(entity.uuid!!).forEach { episodeService.delete(it) }
        super.delete(entity)
    }
}