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

    fun findByName(name: String): Platform? {
        return platformRepository.findByName(name)
    }
}