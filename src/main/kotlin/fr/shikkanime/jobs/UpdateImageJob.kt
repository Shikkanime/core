package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.LoggerFactory

class UpdateImageJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    override fun run() {
        val delay = configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_IMAGE_DELAY, 30) * 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()

        ImageService.removeUnusedImages()

        val images = ImageService.cache.asSequence()
            .filter { entry -> (entry.value.lastUpdateDateTime == null || now - entry.value.lastUpdateDateTime!! >= delay) && !entry.value.url.isNullOrBlank() }
            .shuffled()
            .toList()

        logger.info("Found ${images.size} images to update")

        if (images.isEmpty()) {
            logger.info("No images to update")
            return
        }

        images.take(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_IMAGE_SIZE, 100))
            .forEach { entry ->
                ImageService.add(
                    uuid = entry.key.first,
                    type = entry.key.second,
                    url = entry.value.url!!,
                    bytes = null,
                    bypass = true
                )
            }
    }
}