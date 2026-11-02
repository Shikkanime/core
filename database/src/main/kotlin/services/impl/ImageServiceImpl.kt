package fr.shikkanime.database.services.impl

import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.database.services.ImageService
import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [ImageService::class])
internal class ImageServiceImpl(
    private val imageRepository: ImageRepository
) : ImageService {
    override fun findByEntityIdAndType(entityId: Uuid, type: ImageType): Image? =
        imageRepository.findByEntityIdAndType(entityId, type)
}
