package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.EpisodeEntity
import fr.shikkanime.database.entities.EpisodeTable
import fr.shikkanime.exposed.repositories.AbstractRepository
import fr.shikkanime.models.Platform
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import kotlin.uuid.Uuid

abstract class EpisodeRepository : AbstractRepository<Uuid, EpisodeEntity>(EpisodeEntity) {
    fun findByPlatformAndPlatformEpisodeId(
        platform: Platform,
        platformEpisodeId: String
    ): EpisodeEntity? =
        EpisodeEntity.find {
            (EpisodeTable.platform eq platform) and
                    (EpisodeTable.platformEpisodeId eq platformEpisodeId)
        }.limit(1).firstOrNull()

    fun findAllByPlatformAndPlatformEpisodeIds(
        platform: Platform,
        platformEpisodeIds: List<String>
    ): List<EpisodeEntity> =
        if (platformEpisodeIds.isEmpty()) {
            emptyList()
        } else {
            EpisodeEntity.find {
                (EpisodeTable.platform eq platform) and
                        (EpisodeTable.platformEpisodeId inList platformEpisodeIds)
            }.toList()
        }

    fun saveIfNotExists(
        platform: Platform,
        platformEpisodeId: String,
        title: String,
        releaseDateTime: LocalDateTime
    ): EpisodeEntity =
        findByPlatformAndPlatformEpisodeId(platform, platformEpisodeId)
            .ifExists { /* already persisted: natural key unique, nothing to update */ }
            .newIfNotExists {
                this.platform = platform
                this.platformEpisodeId = platformEpisodeId
                this.title = title
                this.releaseDateTime = releaseDateTime
            }
            .applyFlush()
}
