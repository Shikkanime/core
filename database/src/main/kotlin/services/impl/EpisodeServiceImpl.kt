package fr.shikkanime.database.services.impl

import fr.shikkanime.database.entities.EpisodeEntity
import fr.shikkanime.database.repositories.EpisodeRepository
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.models.Episode
import fr.shikkanime.models.Platform
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Single

@Suppress("unused")
@Single
class EpisodeServiceImpl(
    private val episodeRepository: EpisodeRepository
) : EpisodeService {
    override fun findAllExistingPlatformEpisodeIds(
        platform: Platform,
        platformEpisodeIds: List<String>
    ): Set<String> =
        episodeRepository.findAllByPlatformAndPlatformEpisodeIds(platform, platformEpisodeIds)
            .map { it.platformEpisodeId }
            .toSet()

    override fun save(
        platform: Platform,
        platformEpisodeId: String,
        title: String,
        releaseDateTime: LocalDateTime
    ): Episode =
        episodeRepository
            .saveIfNotExists(platform, platformEpisodeId, title, releaseDateTime)
            .toModel()
}

private fun EpisodeEntity.toModel(): Episode =
    Episode(
        id = id.value,
        createdAt = createdAt,
        updatedAt = updatedAt,
        platform = platform,
        platformEpisodeId = platformEpisodeId,
        title = title,
        releaseDateTime = releaseDateTime
    )
