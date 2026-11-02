package fr.shikkanime.database.repositories.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.entities.EpisodeReleaseEntity
import fr.shikkanime.database.entities.EpisodeReleaseTable
import fr.shikkanime.database.repositories.EpisodeRepository
import fr.shikkanime.database.repositories.EpisodeReleaseRepository
import fr.shikkanime.models.EpisodeRelease
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [EpisodeReleaseRepository::class])
internal class EpisodeReleaseRepositoryImpl(
    private val episodeRepository: EpisodeRepository
) : EpisodeReleaseRepository() {

    override fun findAllByEpisodeIds(episodeIds: List<Uuid>): Map<Uuid?, List<EpisodeRelease>> =
        EpisodeReleaseEntity.find { EpisodeReleaseTable.episode inList episodeIds }
            .map(EpisodeReleaseEntity::toModel)
            .groupBy(EpisodeRelease::episodeId)

    /**
     * Finds the release of an episode playing one audio locale of a platform video.
     *
     * @param episodeId identifier of the episode owning the release.
     * @param platform streaming platform of the release.
     * @param platformId identifier of the video on the streaming platform.
     * @param audioLocale audio track the release plays.
     * @return the matching entity, or `null` when the release is not known yet.
     * @see [EpisodeRelease]
     */
    private fun findByKey(
        episodeId: Uuid,
        platform: Platform,
        platformId: String,
        audioLocale: Locale
    ): EpisodeReleaseEntity? =
        EpisodeReleaseEntity.find {
            (EpisodeReleaseTable.episode eq episodeId) and
                (EpisodeReleaseTable.platform eq platform) and
                (EpisodeReleaseTable.platformId eq platformId) and
                (EpisodeReleaseTable.audioLocale eq audioLocale)
        }
            .limit(1)
            .firstOrNull()

    /**
     * Tells whether a stored release carries different editorial data than the harvested one.
     *
     * @param stored release as currently stored in database.
     * @param candidate release harvested from the platform.
     * @return `true` when one field of the release changed.
     * @see [EpisodeRelease]
     */
    private fun hasChanged(stored: EpisodeReleaseEntity, candidate: EpisodeRelease): Boolean =
        stored.url != candidate.url
            || stored.uncensored != candidate.uncensored
            || stored.releaseDateTime != candidate.releaseDateTime

    override fun save(release: EpisodeRelease): EpisodeRelease =
        findByKey(
            requireNotNull(release.episodeId),
            release.platform,
            release.platformId,
            release.audioLocale
        )
            .ifExists {
                if (hasChanged(this, release)) {
                    url = release.url
                    uncensored = release.uncensored
                    releaseDateTime = release.releaseDateTime
                    updatedAt = TimeUtils.now()
                }
            }
            .newIfNotExists(release.id) {
                // The row belongs to the episode its release already references
                episode = episodeRepository.getReference(requireNotNull(release.episodeId))
                platform = release.platform
                platformId = release.platformId
                url = release.url
                audioLocale = release.audioLocale
                uncensored = release.uncensored
                releaseDateTime = release.releaseDateTime
            }
            .applyFlush()
            .toModel()
}
