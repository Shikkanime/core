package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.entities.Attachment
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.SerializationUtils
import java.util.*

class AttachmentCacheService : ICacheService {
    @Inject private lateinit var attachmentService: AttachmentService

    fun findByEntityUuidTypeAndActive(uuid: UUID, type: ImageType) = MapCache.getOrComputeNullable(
        "AttachmentCacheService.findByEntityUuidTypeAndActive",
        classes = listOf(Attachment::class.java),
        typeToken = object : TypeToken<MapCacheValue<Attachment>>() {},
        serializationType = SerializationUtils.SerializationType.OBJECT,
        key = uuid to type,
    ) { attachmentService.findByEntityUuidTypeAndActive(it.first, it.second) }
}