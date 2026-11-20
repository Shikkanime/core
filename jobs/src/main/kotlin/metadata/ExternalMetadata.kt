package fr.shikkanime.jobs.metadata

import fr.shikkanime.models.MetadataPlatform
import java.time.LocalDate

/**
 * Normalized representation of an anime entry read from an external metadata platform.
 *
 * The shape is platform-agnostic on purpose: AniList, MyAnimeList and AniDB expose different
 * fields, and the linker only needs titles, a description, the episode list and the links the
 * platform itself declares toward the other platforms.
 *
 * @param platform metadata platform owning this entry.
 * @param id identifier of the entry on the platform.
 * @param titles every title the platform knows for the entry, in every language.
 * @param description editorial description, when the platform exposes one.
 * @param episodeCount total number of episodes declared by the platform, when known.
 * @param year first air year of the entry, when known.
 * @param episodes episode entries of the platform, empty when the platform exposes none
 * (AniList has no per-episode data).
 * @param externalIds identifiers of the same entry on the other metadata platforms, when the
 * platform declares them.
 * @param relatedIds identifiers of related entries on the same platform (sequels, prequels,
 * side stories), used to discover the season entries of a franchise.
 */
data class ExternalAnime(
    val platform: MetadataPlatform,
    val id: String,
    val titles: Set<String>,
    val description: String?,
    val episodeCount: Int?,
    val year: Int?,
    val episodes: List<ExternalEpisode>,
    val externalIds: Map<MetadataPlatform, String> = emptyMap(),
    val relatedIds: Set<String> = emptySet()
)

/**
 * Normalized representation of an episode entry of an external metadata platform.
 *
 * @param id identifier of the episode on the platform.
 * @param number episode number inside its external anime, when the platform numbers it.
 * @param title episode title, when the platform exposes one.
 * @param airDate original air date of the episode, when known.
 */
data class ExternalEpisode(
    val id: String,
    val number: Int?,
    val title: String?,
    val airDate: LocalDate?
)
