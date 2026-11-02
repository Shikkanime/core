package fr.shikkanime.database.entities

import fr.shikkanime.models.EpisodeRelease
import fr.shikkanime.models.Locale
import fr.shikkanime.models.ModelConvertible
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.datetime.datetime
import kotlin.uuid.Uuid

const val EPISODE_RELEASE_TABLE_NAME = "episode_release"

internal object EpisodeReleaseTable : ShikkTable(EPISODE_RELEASE_TABLE_NAME) {
    val episode = reference(
        EPISODE_TABLE_ID,
        EpisodeTable,
        fkName = EPISODE_RELEASE_TABLE_NAME + "_" + EPISODE_TABLE_NAME + FK
    )
    val platform = enumerationByName<Platform>("platform", 30)
    val platformId = varchar("platform_id", 255)
    val url = varchar("url", 1000)
    val audioLocale = enumerationByName<Locale>("audio_locale", 10)
    val uncensored = bool("uncensored")
    val releaseDateTime = datetime("release_date_time")

    init {
        // One row per platform video and audio track: a video published in several locales is one
        // row per locale, and the same track of the same video is never stored twice.
        uniqueIndex(
            "${EPISODE_TABLE_ID}_platform_platform_id_audio_locale$UQ",
            episode,
            platform,
            platformId,
            audioLocale
        )
        index("episode_release_platform_id", false, platform, platformId)
    }
}

internal class EpisodeReleaseEntity(id: EntityID<Uuid>) :
    ShikkEntity(id, EpisodeReleaseTable),
    ModelConvertible<EpisodeRelease> {
    companion object : UuidEntityClass<EpisodeReleaseEntity>(EpisodeReleaseTable)

    var episode by EpisodeReleaseTable.episode
    var platform by EpisodeReleaseTable.platform
    var platformId by EpisodeReleaseTable.platformId
    var url by EpisodeReleaseTable.url
    var audioLocale by EpisodeReleaseTable.audioLocale
    var uncensored by EpisodeReleaseTable.uncensored
    var releaseDateTime by EpisodeReleaseTable.releaseDateTime

    override fun toModel(): EpisodeRelease =
        EpisodeRelease(
            id = id.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            episodeId = episode.value,
            platform = platform,
            platformId = platformId,
            url = url,
            audioLocale = audioLocale,
            uncensored = uncensored,
            releaseDateTime = releaseDateTime
        )
}
