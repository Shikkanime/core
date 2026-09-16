package fr.shikkanime.database.services

import fr.shikkanime.models.Episode
import fr.shikkanime.models.Platform
import fr.shikkanime.exposed.Transactional
import kotlinx.datetime.LocalDateTime

interface EpisodeService {
    @Transactional
    fun findAllExistingPlatformEpisodeIds(
        platform: Platform,
        platformEpisodeIds: List<String>
    ): Set<String>

    @Transactional
    fun save(
        platform: Platform,
        platformEpisodeId: String,
        title: String,
        releaseDateTime: LocalDateTime
    ): Episode
}
