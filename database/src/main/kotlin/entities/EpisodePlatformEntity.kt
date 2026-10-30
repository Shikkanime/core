package fr.shikkanime.database.entities

import fr.shikkanime.database.enumSet
import fr.shikkanime.models.EpisodePlatform
import fr.shikkanime.models.Locale
import fr.shikkanime.models.ModelConvertible
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.datetime.datetime
import kotlin.uuid.Uuid

const val EPISODE_PLATFORM_TABLE_NAME = "episode_platform"

internal object EpisodePlatformTable : ShikkTable(EPISODE_PLATFORM_TABLE_NAME) {
    val episode = reference(
        EPISODE_TABLE_ID,
        EpisodeTable,
        fkName = EPISODE_PLATFORM_TABLE_NAME + "_" + EPISODE_TABLE_NAME + FK
    )
    val platform = enumerationByName<Platform>("platform", 30)
    val platformId = varchar("platform_id", 255)
    val url = varchar("url", 1000)
    val audioLocales = enumSet<Locale>("audio_locales")
    val uncensored = bool("uncensored")
    val releaseDateTime = datetime("release_date_time")

    init {
        // One row per platform video: the same episode can carry several videos on one platform
        // (a subtitled and a dubbed release are two identifiers), never the same one twice.
        uniqueIndex("${EPISODE_TABLE_ID}_platform_platform_id$UQ", episode, platform, platformId)
        index("episode_platform_platform_id", false, platform, platformId)
    }
}

internal class EpisodePlatformEntity(id: EntityID<Uuid>) :
    ShikkEntity(id, EpisodePlatformTable),
    ModelConvertible<EpisodePlatform> {
    companion object : UuidEntityClass<EpisodePlatformEntity>(EpisodePlatformTable)

    var episode by EpisodePlatformTable.episode
    var platform by EpisodePlatformTable.platform
    var platformId by EpisodePlatformTable.platformId
    var url by EpisodePlatformTable.url
    var audioLocales by EpisodePlatformTable.audioLocales
    var uncensored by EpisodePlatformTable.uncensored
    var releaseDateTime by EpisodePlatformTable.releaseDateTime

    override fun toModel(): EpisodePlatform =
        EpisodePlatform(
            id = id.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            episodeId = episode.value,
            platform = platform,
            platformId = platformId,
            url = url,
            audioLocales = audioLocales,
            uncensored = uncensored,
            releaseDateTime = releaseDateTime
        )
}
