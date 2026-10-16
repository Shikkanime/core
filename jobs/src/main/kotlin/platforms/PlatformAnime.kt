package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.Platform

/**
 * Normalized representation of an anime title harvested from a streaming platform.
 *
 * This entity never carries translatable text nor fixed artwork fields: titles and synopses live
 * in [PlatformAnimeLocalization] and artwork in [PlatformImage], so new languages and image kinds
 * are added without reshaping it.
 *
 * @property id Identifier of the show assigned by the source platform.
 * @property platform Streaming platform this show was harvested from.
 * @property images Artwork made available by the platform for this show.
 * @property localizations Localized editorial texts of the show, one per eligible country.
 */
data class PlatformAnime(
    val id: String,
    val platform: Platform,
    val images: List<PlatformImage>,
    val localizations: List<PlatformAnimeLocalization>
) {
    /**
     * Maps this harvested show to the canonical anime aggregate to persist.
     *
     * The mapping is a total function of this harvest: it reads only the constructor state and
     * invents nothing. The row does not exist yet, so the identity and audit columns stay empty and
     * the persistence fills them; on an update the stored identity is re-applied by the caller.
     * The platform source link is rebuilt here, and the children are mapped through their own
     * [PlatformImage.toModel] and [PlatformAnimeLocalization.toModel].
     *
     * @return the [Anime] to pass to the persistence.
     * @see Anime
     */
    fun toModel(): Anime =
        Anime(
            id = null,
            createdAt = null,
            updatedAt = null,
            images = images.map(PlatformImage::toModel),
            localizations = localizations.map(PlatformAnimeLocalization::toModel),
            platforms = listOf(
                AnimePlatform(
                    id = null,
                    createdAt = null,
                    updatedAt = null,
                    platform = platform,
                    platformId = this.id
                )
            )
        )
}
