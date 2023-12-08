package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Platform
import fr.shikkanime.repositories.PlatformRepository

class PlatformService : AbstractService<Platform, PlatformRepository>() {
    @Inject
    private lateinit var platformRepository: PlatformRepository

    override fun getRepository(): PlatformRepository {
        return platformRepository
    }

    fun findByName(name: String?): Platform? {
        if (name.isNullOrBlank()) {
            return null
        }

        return platformRepository.findByName(name)
    }

    override fun saveOrUpdate(entity: Platform): Platform {
        val entityFromDb = findByName(entity.name) ?: return save(entity)

        if (entityFromDb.url != entity.url) {
            entityFromDb.url = entity.url
        }

        if (entityFromDb.image != entity.image) {
            entityFromDb.image = entity.image
        }

        return super.update(entityFromDb)
    }
}