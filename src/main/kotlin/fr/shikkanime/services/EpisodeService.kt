package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Episode
import fr.shikkanime.repositories.EpisodeRepository

class EpisodeService : AbstractService<Episode, EpisodeRepository>() {
    @Inject
    private lateinit var episodeRepository: EpisodeRepository

    @Inject
    private lateinit var platformService: PlatformService

    @Inject
    private lateinit var animeService: AnimeService

    override fun getRepository(): EpisodeRepository {
        return episodeRepository
    }

    fun findByHash(hash: String?): Episode? {
        return episodeRepository.findByHash(hash)
    }

    override fun saveOrUpdate(entity: Episode): Episode {
        entity.platform = platformService.saveOrUpdate(entity.platform!!)
        entity.anime = animeService.saveOrUpdate(entity.anime!!)

        val entityFromDb = findByHash(entity.hash) ?: return save(entity)

        if (entityFromDb.season != entity.season) {
            entityFromDb.season = entity.season
        }

        if (entityFromDb.number != entity.number) {
            entityFromDb.number = entity.number
        }

        if (entityFromDb.title != entity.title) {
            entityFromDb.title = entity.title
        }

        if (entityFromDb.url != entity.url) {
            entityFromDb.url = entity.url
        }

        if (entityFromDb.image != entity.image) {
            entityFromDb.image = entity.image
        }

        if (entityFromDb.duration != entity.duration) {
            entityFromDb.duration = entity.duration
        }

        return super.update(entityFromDb)
    }
}