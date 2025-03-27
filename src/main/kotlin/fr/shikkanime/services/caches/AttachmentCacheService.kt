package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Attachment
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.utils.MapCache
import java.util.*

class AttachmentCacheService : AbstractCacheService {
    @Inject
    private lateinit var attachmentService: AttachmentService

    fun findByEntityUuidTypeAndActive(uuid: UUID, type: ImageType) = MapCache.getOrComputeNullable(
        "AttachmentCacheService.findByEntityUuidTypeAndActive",
        classes = listOf(Attachment::class.java),
        key = uuid to type,
    ) { attachmentService.findByEntityUuidTypeAndActive(it.first, it.second) }
}