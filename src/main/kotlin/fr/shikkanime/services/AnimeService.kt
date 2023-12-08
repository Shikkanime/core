package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.repositories.AnimeRepository

class AnimeService : AbstractService<Anime, AnimeRepository>() {
    @Inject
    private lateinit var animeRepository: AnimeRepository

    override fun getRepository(): AnimeRepository {
        return animeRepository
    }

    fun findByName(name: String?): Anime? {
        return animeRepository.findByName(name)
    }

    override fun saveOrUpdate(entity: Anime): Anime {
        val entityFromDb = findByName(entity.name) ?: return save(entity)

        if (entityFromDb.image != entity.image) {
            entityFromDb.image = entity.image
        }

        if (entityFromDb.description != entity.description) {
            entityFromDb.description = entity.description
        }

        return super.update(entityFromDb)
    }
}