package fr.shikkanime.admin.dtos.episode

import fr.shikkanime.models.EpisodeMetadataLink
import fr.shikkanime.models.MetadataPlatform
import kotlinx.serialization.Serializable

/**
 * Link of a stored episode to its editorial entry on an external metadata platform.
 *
 * [platformAnimeId] is the identifier of the parent anime entry on the same platform: episode
 * identifiers are only unique inside their anime on some platforms (MyAnimeList), so the pair
 * is what locates the episode on the platform's website.
 */
@Serializable
data class AdminEpisodeMetadataLinkDto(
    val platform: MetadataPlatform,
    val platformId: String,
    val platformAnimeId: String?
)

/**
 * Maps a stored episode metadata link to its administration view.
 *
 * @receiver stored link between an episode and a metadata platform.
 * @return the administration view of this link.
 * @see [AdminEpisodeMetadataLinkDto]
 * @see [EpisodeMetadataLink]
 */
fun EpisodeMetadataLink.toAdminDto(): AdminEpisodeMetadataLinkDto =
    AdminEpisodeMetadataLinkDto(
        platform = platform,
        platformId = platformId,
        platformAnimeId = platformAnimeId
    )
