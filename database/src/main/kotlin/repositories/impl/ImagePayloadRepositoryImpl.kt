package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.repositories.ImagePayloadRepository
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [ImagePayloadRepository::class])
internal class ImagePayloadRepositoryImpl : ImagePayloadRepository() {

    override fun findByImageId(imageId: Uuid): ByteArray? =
        findById(imageId)?.bytes

    override fun delete(imageId: Uuid) {
        findById(imageId)?.delete()
    }

    override fun save(imageId: Uuid, bytes: ByteArray) {
        findById(imageId)
            .ifExists { this.bytes = bytes }
            .newIfNotExists(imageId) { this.bytes = bytes }
            .applyFlush()
    }
}
