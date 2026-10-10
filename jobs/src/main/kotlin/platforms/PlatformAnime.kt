package fr.shikkanime.jobs.platforms

/**
 * Normalized representation of an anime title harvested from a streaming platform.
 *
 * @property id Identifier of the show assigned by the source platform.
 * @property title Canonical cleaned title of the anime.
 * @property description Optional overview or synopsis of the series.
 * @property thumbnail Portrait cover art or poster URL.
 * @property banner Optional landscape banner artwork URL.
 */
data class PlatformAnime(
    val id: String,
    val title: String,
    val description: String?,
    val thumbnail: String,
    val banner: String?
)
