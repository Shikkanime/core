package fr.shikkanime.admin.usecases

import fr.shikkanime.admin.dtos.image.AdminImageDto
import fr.shikkanime.models.ImageType
import kotlin.uuid.Uuid

/**
 * Manages the artwork of a stored anime for the administration API.
 *
 * An overridden image is marked as manually managed, so a platform harvest never replaces
 * its URL; releasing it hands the artwork back to the harvests.
 */
interface AnimeImageUseCase {
    /**
     * Overrides the artwork of an anime with the given URL.
     *
     * @param animeId identifier of the anime owning the artwork.
     * @param type type of the artwork to override.
     * @param url URL of the replacement artwork.
     * @return the overridden image, or `null` when no anime carries this identifier.
     * @see [AdminImageDto]
     */
    fun update(animeId: Uuid, type: ImageType, url: String): AdminImageDto?

    /**
     * Releases the manual management of the artwork of an anime.
     *
     * The current URL is kept until a platform harvest replaces it.
     *
     * @param animeId identifier of the anime owning the artwork.
     * @param type type of the artwork to release.
     * @return the released image, or `null` when the anime has no artwork of this type.
     * @see [AdminImageDto]
     */
    fun reset(animeId: Uuid, type: ImageType): AdminImageDto?
}
