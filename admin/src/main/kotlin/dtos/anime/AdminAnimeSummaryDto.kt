package fr.shikkanime.admin.dtos.anime

import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Platform
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * Compact administration view of one anime in the catalog list.
 *
 * The payload only carries what a list row displays: the title, the normalized identity, the
 * platforms it comes from and the number of translations. Descriptions, image URLs and platform
 * identifiers are loaded with [AdminAnimeDetailDto] when the administrator opens the anime.
 */
@Serializable
data class AdminAnimeSummaryDto(
    val id: Uuid,
    val title: String,
    val normalizedTitle: String,
    val platforms: List<Platform>,
    val localizationCount: Int
)

/**
 * Maps a stored anime aggregate to its compact list view.
 *
 * The displayed title is the first French localization, falling back to the first available
 * translation and then to the normalized title, so a row always has something to show.
 *
 * @receiver stored anime with the rows it owns.
 * @return the compact administration view of this anime.
 * @see [AdminAnimeSummaryDto]
 * @see [AnimeWithChildren]
 */
fun AnimeWithChildren.toAdminSummaryDto(): AdminAnimeSummaryDto =
    AdminAnimeSummaryDto(
        id = requireNotNull(anime.id),
        title = localizations.firstOrNull { it.locale.name.startsWith("FR") }?.title
            ?: localizations.firstOrNull()?.title
            ?: anime.normalizedTitle,
        normalizedTitle = anime.normalizedTitle,
        platforms = platforms.map { it.platform }.distinct(),
        localizationCount = localizations.size
    )
