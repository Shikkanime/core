package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Episode
import fr.shikkanime.repositories.EpisodeRepository

class EpisodeService : AbstractService<Episode, EpisodeRepository>() {
    @Inject
    private lateinit var episodeRepository: EpisodeRepository

    @Inject
    private lateinit var animeService: AnimeService

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
        entity.anime = animeService.findByName(entity.anime!!.countryCode!!, entity.anime!!.name!!)
            ?: animeService.save(entity.anime!!)
        return super.save(entity)
    }
}