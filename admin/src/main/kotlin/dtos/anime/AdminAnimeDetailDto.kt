package fr.shikkanime.admin.dtos.anime

import fr.shikkanime.admin.dtos.image.AdminImageDto
import fr.shikkanime.admin.dtos.localization.AdminLocalizationDto
import fr.shikkanime.models.*
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid
import fr.shikkanime.admin.dtos.image.toAdminDto as toImageAdminDto
import fr.shikkanime.admin.dtos.localization.toAdminDto as toLocalizationAdminDto

/**
 * Full administration view of one anime.
 *
 * The detail screen shows everything the database stores about the anime: its identity, its audit
 * columns, its localized texts with their full descriptions, its platform source links, its
 * metadata platform links and its artwork URLs. The list screen uses [AdminAnimeSummaryDto]
 * instead, so a catalog page does not carry descriptions and image URLs it would not display.
 */
@Serializable
data class AdminAnimeDetailDto(
    val id: Uuid,
    val normalizedTitle: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val localizations: List<AdminLocalizationDto>,
    val platforms: List<AdminAnimePlatformDto>,
    val images: List<AdminImageDto>,
    val metadataLinks: List<AdminAnimeMetadataLinkDto>
)

/**
 * An anime aggregate paired with its metadata platform links.
 *
 * The detail view joins two aggregates the persistence keeps apart, so the mapping receives
 * them composed here instead of through a parameter.
 *
 * @property anime stored anime with the rows it owns.
 * @property metadataLinks links of the anime to the external metadata platforms.
 * @see [AdminAnimeDetailDto]
 */
data class AnimeWithMetadataLinks(
    val anime: AnimeWithChildren,
    val metadataLinks: List<AnimeMetadataLink>
)

/**
 * Maps a stored anime aggregate and its metadata links to the full administration view.
 *
 * @receiver stored anime with its rows and metadata links.
 * @return the full administration view of this anime.
 * @see [AdminAnimeDetailDto]
 * @see [AnimeWithMetadataLinks]
 */
fun AnimeWithMetadataLinks.toAdminDetailDto(): AdminAnimeDetailDto =
    AdminAnimeDetailDto(
        id = requireNotNull(anime.id),
        normalizedTitle = anime.normalizedTitle,
        createdAt = anime.createdAt,
        updatedAt = anime.updatedAt,
        localizations = anime.localizations.map(AnimeLocalization::toLocalizationAdminDto),
        platforms = anime.platforms.map(AnimePlatform::toAdminDto),
        images = anime.images.map(Image::toImageAdminDto),
        metadataLinks = metadataLinks.map(AnimeMetadataLink::toAdminDto)
    )
