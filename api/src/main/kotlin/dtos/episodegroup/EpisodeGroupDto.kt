package fr.shikkanime.api.dtos.episodegroup

import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeGroupDetail
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.LangType
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * API view of a group of episode releases published together for one market.
 *
 * The group identity is its anime, its episode type and its release date; the
 * coverage lists every lang type present with the episode coordinates it
 * covers, and the releases carry the platform details of every member.
 */
@Serializable
data class EpisodeGroupDto(
    val animeId: Uuid,
    val animeTitle: String,
    val episodeType: EpisodeType,
    val releaseDateTime: LocalDateTime,
    val minSeason: Int,
    val maxSeason: Int,
    val minNumber: Int,
    val maxNumber: Int,
    val langTypes: List<LangType>,
    val episodes: List<EpisodeGroupEpisodeDto>,
    val releases: List<EpisodeGroupReleaseDto>
)

/**
 * API view of one episode covered by an [EpisodeGroupDto].
 */
@Serializable
data class EpisodeGroupEpisodeDto(
    val episodeId: Uuid,
    val season: Int,
    val episodeType: EpisodeType,
    val number: Int
)

/**
 * API view of one release member of an [EpisodeGroupDto].
 */
@Serializable
data class EpisodeGroupReleaseDto(
    val releaseId: Uuid,
    val platform: Platform,
    val platformId: String,
    val url: String,
    val audioLocale: Locale,
    val uncensored: Boolean,
    val langType: LangType,
    val releaseDateTime: LocalDateTime
)

/**
 * Maps a hydrated group to its API view.
 *
 * @param country market the lang type of each release resolves for.
 * @return the API view of this group.
 * @see [EpisodeGroupDto]
 */
fun EpisodeGroupDetail.toDto(country: Country): EpisodeGroupDto {
    val group = this.group
    return EpisodeGroupDto(
        animeId = group.animeId,
        animeTitle = group.animeSlug,
        episodeType = group.episodeType,
        releaseDateTime = group.releaseDateTime,
        minSeason = group.minSeason,
        maxSeason = group.maxSeason,
        minNumber = group.minNumber,
        maxNumber = group.maxNumber,
        langTypes = group.coverage.keys.sortedBy(LangType::ordinal),
        episodes = group.episodes.map { episode ->
            EpisodeGroupEpisodeDto(
                episodeId = episode.episodeId,
                season = episode.season,
                episodeType = episode.episodeType,
                number = episode.number
            )
        },
        releases = group.releases.map { observation ->
            val release = releases.getValue(observation.releaseId)
            EpisodeGroupReleaseDto(
                releaseId = observation.releaseId,
                platform = release.platform,
                platformId = release.platformId,
                url = release.url,
                audioLocale = release.audioLocale,
                uncensored = release.uncensored,
                langType = LangType.fromAudioLocale(country, release.audioLocale),
                releaseDateTime = release.releaseDateTime
            )
        }
    )
}
