package fr.shikkanime.admin.dtos.anime

import fr.shikkanime.admin.dtos.image.AdminImageDto
import fr.shikkanime.admin.dtos.image.toAdminDto as toImageAdminDto
import fr.shikkanime.admin.dtos.localization.AdminLocalizationDto
import fr.shikkanime.admin.dtos.localization.toAdminDto as toLocalizationAdminDto
import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Image
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * Full administration view of one anime.
 *
 * The detail screen shows everything the database stores about the anime: its identity, its audit
 * columns, its localized texts with their full descriptions, its platform source links and its
 * artwork URLs. The list screen uses [AdminAnimeSummaryDto] instead, so a catalog page does not
 * carry descriptions and image URLs it would not display.
 */
@Serializable
data class AdminAnimeDetailDto(
    val id: Uuid,
    val normalizedTitle: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val localizations: List<AdminLocalizationDto>,
    val platforms: List<AdminAnimePlatformDto>,
    val images: List<AdminImageDto>
)

/**
 * Maps a stored anime aggregate to its full administration view.
 *
 * @receiver stored anime with the rows it owns.
 * @return the full administration view of this anime.
 * @see [AdminAnimeDetailDto]
 * @see [AnimeWithChildren]
 */
fun AnimeWithChildren.toAdminDetailDto(): AdminAnimeDetailDto =
    AdminAnimeDetailDto(
        id = requireNotNull(anime.id),
        normalizedTitle = anime.normalizedTitle,
        createdAt = anime.createdAt,
        updatedAt = anime.updatedAt,
        localizations = localizations.map(AnimeLocalization::toLocalizationAdminDto),
        platforms = platforms.map(AnimePlatform::toAdminDto),
        images = images.map(Image::toImageAdminDto)
    )
