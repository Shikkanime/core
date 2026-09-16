package fr.shikkanime.database.entities

import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.datetime.datetime
import kotlin.uuid.Uuid

const val EPISODE_TABLE_NAME = "episode"
const val EPISODE_TABLE_ID = EPISODE_TABLE_NAME + ID

object EpisodeTable : ShikkTable(EPISODE_TABLE_NAME) {
    val platform = enumerationByName<Platform>("platform", 64)
    val platformEpisodeId = varchar("platform_episode_id", 255)
    val title = varchar("title", 255)
    val releaseDateTime = datetime("release_date_time")

    init {
        uniqueIndex(EPISODE_TABLE_NAME + "_platform_platform_episode_id$UQ", platform, platformEpisodeId)
    }
}

class EpisodeEntity(id: EntityID<Uuid>) : ShikkEntity(id, EpisodeTable) {
    companion object : UuidEntityClass<EpisodeEntity>(EpisodeTable)

    var platform by EpisodeTable.platform
    var platformEpisodeId by EpisodeTable.platformEpisodeId
    var title by EpisodeTable.title
    var releaseDateTime by EpisodeTable.releaseDateTime
}
