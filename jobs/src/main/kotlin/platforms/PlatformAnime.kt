package fr.shikkanime.jobs.platforms

/**
 * Normalized representation of an anime title harvested from a streaming platform.
 *
 * This entity never carries translatable text nor fixed artwork fields: titles and synopses live
 * in [PlatformAnimeLocalization] and artwork in [PlatformImage], so new languages and image kinds
 * are added without reshaping it.
 *
 * @property id Identifier of the show assigned by the source platform.
 * @property images Artwork made available by the platform for this show.
 * @property localizations Localized editorial texts of the show, one per eligible country.
 */
data class PlatformAnime(
    val id: String,
    val images: List<PlatformImage>,
    val localizations: List<PlatformAnimeLocalization>
)
