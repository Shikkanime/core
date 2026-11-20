package fr.shikkanime.admin.dtos.anime

import fr.shikkanime.models.AnimeMetadataLink
import fr.shikkanime.models.MetadataPlatform
import kotlinx.serialization.Serializable

/**
 * Link of a stored anime to its editorial entry on an external metadata platform.
 *
 * Unlike [AdminAnimePlatformDto], which references where episodes stream from, this link only
 * points at the metadata entry (AniList, MyAnimeList, AniDB) used to cross-check the catalog.
 */
@Serializable
data class AdminAnimeMetadataLinkDto(
    val platform: MetadataPlatform,
    val platformId: String
)

/**
 * Maps a stored anime metadata link to its administration view.
 *
 * @receiver stored link between an anime and a metadata platform.
 * @return the administration view of this link.
 * @see [AdminAnimeMetadataLinkDto]
 * @see [AnimeMetadataLink]
 */
fun AnimeMetadataLink.toAdminDto(): AdminAnimeMetadataLinkDto =
    AdminAnimeMetadataLinkDto(
        platform = platform,
        platformId = platformId
    )
