package fr.shikkanime.jobs.platforms

import fr.shikkanime.models.Episode
import fr.shikkanime.models.EpisodePlatform
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.EpisodeWithChildren
import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Locale
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.ZonedDateTime
import kotlin.time.Instant

/**
 * Normalized representation of an episode harvested from a streaming platform.
 */
data class PlatformEpisode(
    val anime: PlatformAnime,
    val id: String,
    val image: String,
    val releaseDateTime: ZonedDateTime,
    val season: Int,
    val number: Int,
    val episodeType: EpisodeType,
    val duration: Long,
    val url: String,
    val audioLocales: List<Locale>,
    val uncensored: Boolean,
    val localizations: List<PlatformEpisodeLocalization>
) {
    /**
     * Maps this harvested episode to its platform release.
     *
     * The release is persisted whatever source owns the episode's metadata: a lower priority
     * platform still publishes its own video and must be stored.
     *
     * @return the [EpisodePlatform] to pass to the persistence.
     * @see [EpisodePlatform]
     */
    fun toPlatformModel(): EpisodePlatform =
        EpisodePlatform(
            id = null,
            createdAt = null,
            updatedAt = null,
            episodeId = null,
            platform = anime.platform,
            platformId = id,
            url = url,
            audioLocales = audioLocales.toSet(),
            uncensored = uncensored,
            releaseDateTime = releaseDateTime.toUtcLocalDateTime()
        )

    /**
     * Maps this harvested episode to the canonical aggregate to persist.
     *
     * The mapping is a total function of this harvest: it reads only the constructor state and
     * invents nothing. The row does not exist yet, so the identity and audit columns stay empty and
     * the persistence fills them; the owning anime is attached by the caller, which is the step that
     * saved it. The children carry no owner either: the persistence attaches them to the episode it
     * just saved.
     *
     * @return the [EpisodeWithChildren] to pass to the persistence.
     * @see [EpisodeWithChildren]
     */
    fun toModel(): EpisodeWithChildren =
        EpisodeWithChildren(
            episode = Episode(
                id = null,
                createdAt = null,
                updatedAt = null,
                animeId = null,
                season = season,
                number = number,
                episodeType = episodeType,
                releaseDateTime = releaseDateTime.toUtcLocalDateTime(),
                duration = duration
            ),
            images = listOf(
                Image(
                    id = null,
                    createdAt = null,
                    updatedAt = null,
                    entityId = null,
                    type = ImageType.BANNER,
                    url = image
                )
            ),
            localizations = localizations.map(PlatformEpisodeLocalization::toModel),
            platforms = listOf(toPlatformModel())
        )
}

/**
 * Converts this release date to the UTC local date-time the persistence stores.
 *
 * The harvest dates carry the offset of their platform; the database keeps every date in UTC.
 *
 * @receiver release date of the harvest, with its platform offset.
 * @return the same instant, as a UTC local date-time.
 * @see [Episode]
 */
private fun ZonedDateTime.toUtcLocalDateTime(): LocalDateTime =
    Instant.fromEpochMilliseconds(toInstant().toEpochMilli())
        .toLocalDateTime(TimeZone.UTC)
