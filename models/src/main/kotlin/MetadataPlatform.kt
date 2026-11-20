package fr.shikkanime.models

/**
 * External metadata databases an anime or an episode can be linked to.
 *
 * Unlike [Platform], which identifies where an episode is streamed from, a metadata platform
 * only references the editorial entry of an anime or an episode: no release, no audio locale,
 * no censorship flag.
 *
 * @see [AnimeMetadataLink]
 * @see [EpisodeMetadataLink]
 */
enum class MetadataPlatform {
    ANILIST,
    MYANIMELIST,
    ANIDB,
}
