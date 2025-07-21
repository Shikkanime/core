package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Attachment
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.TelemetryConfig
import fr.shikkanime.utils.TelemetryConfig.trace
import java.util.*

class AttachmentCacheService : ICacheService {
    private val tracer = TelemetryConfig.getTracer("AttachmentCacheService")
    @Inject private lateinit var attachmentService: AttachmentService

    fun findByEntityUuidTypeAndActive(uuid: UUID, type: ImageType) = MapCache.getOrComputeNullable(
        "AttachmentCacheService.findByEntityUuidTypeAndActive",
        classes = listOf(Attachment::class.java),
        key = uuid to type,
    ) { tracer.trace { attachmentService.findByEntityUuidTypeAndActive(it.first, it.second) } }
}