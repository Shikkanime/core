package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.caches.ConfigCacheService
import java.util.*

class UpdateImageJob : AbstractJob {
    @Inject
    private lateinit var configCacheService: ConfigCacheService

    override fun run() {
        val delay = configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_IMAGE_DELAY, 30) * 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()

        ImageService.removeUnusedImages()

        ImageService.cache.asSequence()
            .filter { entry -> (entry.value.lastUpdateDateTime == null || now - entry.value.lastUpdateDateTime!! > delay) && !entry.value.url.isNullOrBlank() && entry.value.width != null && entry.value.height != null }
            .shuffled()
            .take(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_IMAGE_SIZE, 100))
            .forEach { entry -> ImageService.add(UUID.fromString(entry.key.first), entry.key.second, entry.value.url!!, entry.value.width!!, entry.value.height!!, bypass = true) }
    }
}