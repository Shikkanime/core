package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Platform
import fr.shikkanime.models.canonicalTitle

/**
 * Normalized representation of an anime title harvested from a streaming platform.
 *
 * This entity never carries translatable text nor fixed artwork fields: titles and synopses live
 * in [PlatformAnimeLocalization] and artwork in [PlatformImage], so new languages and image kinds
 * are added without reshaping it.
 *
 */
data class PlatformAnime(
    val id: String,
    val platform: Platform,
    val images: List<PlatformImage>,
    val localizations: List<PlatformAnimeLocalization>
) {
    // Identity title this anime is looked up and deduplicated with, computed from its localizations
    val normalizedTitle: String by lazy { canonicalTitle(localizations.associate { it.locale to it.title }) }

    /**
     * Maps this harvested show to its platform source link.
     *
     * The link is persisted whatever source owns the anime's metadata: a lower priority platform
     * still publishes its own identifier and must be stored.
     *
     * @return the [AnimePlatform] to pass to the persistence.
     * @see [AnimePlatform]
     */
    fun toPlatformModel(): AnimePlatform =
        AnimePlatform(
            id = null,
            createdAt = null,
            updatedAt = null,
            animeId = null,
            platform = platform,
            platformId = id
        )

    /**
     * Maps this harvested show to the canonical aggregate to persist.
     *
     * The mapping is a total function of this harvest: it reads only the constructor state and
     * invents nothing. The row does not exist yet, so the identity and audit columns stay empty and
     * the persistence fills them; on an update the stored identity is re-applied by the caller. The
     * children carry no owner either: the persistence attaches them to the anime it just saved.
     *
     * @return the [AnimeWithChildren] to pass to the persistence.
     * @see [AnimeWithChildren]
     */
    fun toModel(): AnimeWithChildren =
        AnimeWithChildren(
            anime = Anime(
                id = null,
                createdAt = null,
                updatedAt = null,
                normalizedTitle = normalizedTitle
            ),
            images = images.map(PlatformImage::toModel),
            localizations = localizations.map(PlatformAnimeLocalization::toModel),
            platforms = listOf(toPlatformModel())
        )
}
