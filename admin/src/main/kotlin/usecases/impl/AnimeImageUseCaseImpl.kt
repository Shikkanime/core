package fr.shikkanime.admin.usecases.impl

import fr.shikkanime.admin.dtos.image.AdminImageDto
import fr.shikkanime.admin.dtos.image.toAdminDto
import fr.shikkanime.admin.usecases.AnimeImageUseCase
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.models.ImageType
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single
class AnimeImageUseCaseImpl(
    private val animeService: AnimeService
) : AnimeImageUseCase {
    override fun update(animeId: Uuid, type: ImageType, url: String): AdminImageDto? {
        val sanitizedUrl = url.trim()

        // The artwork URL is rendered by the interface: only remote http(s) assets are accepted
        require(sanitizedUrl.startsWith("https://") || sanitizedUrl.startsWith("http://")) {
            "Image URL must start with http:// or https://"
        }
        require(sanitizedUrl.length <= MAX_URL_LENGTH) {
            "Image URL is too long"
        }

        return animeService.saveManualImage(animeId, type, sanitizedUrl)?.toAdminDto()
    }

    override fun reset(animeId: Uuid, type: ImageType): AdminImageDto? =
        animeService.clearManualImage(animeId, type)?.toAdminDto()

    private companion object {
        // Matches the url column length of the image table
        const val MAX_URL_LENGTH = 1000
    }
}
