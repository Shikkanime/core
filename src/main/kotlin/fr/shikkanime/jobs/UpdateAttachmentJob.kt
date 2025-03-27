package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.Attachment
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.withUTC
import java.time.ZonedDateTime

class UpdateAttachmentJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Inject
    private lateinit var attachmentService: AttachmentService

    override fun run() {
        val zonedDateTime = ZonedDateTime.now().withSecond(0).withNano(0).withUTC()
        val lastUpdateDateTime = zonedDateTime.minusDays(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_ATTACHMENT_DELAY, 30).toLong())
        
        attachmentService.cleanUnusedAttachments()
        val attachments = attachmentService.findAllNeededUpdate(lastUpdateDateTime).shuffled().toList()

        logger.info("Found ${attachments.size} attachments to update")

        if (attachments.isEmpty()) {
            logger.info("No attachments to update")
            return
        }

        attachments.take(configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_ATTACHMENT_SIZE, 100))
            .forEach { attachment ->
                attachment.lastUpdateDateTime = zonedDateTime
                attachmentService.update(attachment)
                attachmentService.encodeAttachment(attachment, attachment.url, null)
            }

        MapCache.invalidate(Attachment::class.java)
    }
}