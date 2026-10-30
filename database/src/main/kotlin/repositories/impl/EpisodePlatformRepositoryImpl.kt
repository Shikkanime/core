package fr.shikkanime.database.repositories.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.entities.EpisodePlatformEntity
import fr.shikkanime.database.entities.EpisodePlatformTable
import fr.shikkanime.database.entities.EpisodeTable
import fr.shikkanime.database.repositories.EpisodePlatformRepository
import fr.shikkanime.models.EpisodePlatform
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [EpisodePlatformRepository::class])
internal class EpisodePlatformRepositoryImpl : EpisodePlatformRepository() {

    override fun findAllByEpisodeIds(episodeIds: List<Uuid>): Map<Uuid?, List<EpisodePlatform>> =
        EpisodePlatformEntity.find { EpisodePlatformTable.episode inList episodeIds }
            .map(EpisodePlatformEntity::toModel)
            .groupBy(EpisodePlatform::episodeId)

    /**
     * Finds one platform release of an episode by its platform and platform identifier.
     *
     * @param episodeId identifier of the episode owning the release.
     * @param platform streaming platform of the release.
     * @param platformId identifier of the episode on the streaming platform.
     * @return the matching entity, or `null` when the release is not known yet.
     * @see [EpisodePlatform]
     */
    private fun findByIdPlatformAndPlatformId(
        episodeId: Uuid,
        platform: Platform,
        platformId: String
    ): EpisodePlatformEntity? =
        EpisodePlatformEntity.find {
            (EpisodePlatformTable.episode eq episodeId) and
                (EpisodePlatformTable.platform eq platform) and
                (EpisodePlatformTable.platformId eq platformId)
        }
            .limit(1)
            .firstOrNull()

    /**
     * Tells whether a stored release carries different editorial data than the harvested one.
     *
     * @param stored release as currently stored in database.
     * @param candidate release harvested from the platform.
     * @return `true` when one field of the release changed.
     * @see [EpisodePlatform]
     */
    private fun hasChanged(stored: EpisodePlatformEntity, candidate: EpisodePlatform): Boolean =
        stored.url != candidate.url
            || stored.audioLocales != candidate.audioLocales
            || stored.uncensored != candidate.uncensored
            || stored.releaseDateTime != candidate.releaseDateTime

    override fun save(episodePlatform: EpisodePlatform): EpisodePlatform =
        findByIdPlatformAndPlatformId(
            requireNotNull(episodePlatform.episodeId),
            episodePlatform.platform,
            episodePlatform.platformId
        )
            .ifExists {
                if (hasChanged(this, episodePlatform)) {
                    url = episodePlatform.url
                    audioLocales = episodePlatform.audioLocales
                    uncensored = episodePlatform.uncensored
                    releaseDateTime = episodePlatform.releaseDateTime
                    updatedAt = TimeUtils.now()
                }
            }
            .newIfNotExists(episodePlatform.id) {
                // The row belongs to the episode its platform release already references
                episode = EntityID(requireNotNull(episodePlatform.episodeId), EpisodeTable)
                platform = episodePlatform.platform
                platformId = episodePlatform.platformId
                url = episodePlatform.url
                audioLocales = episodePlatform.audioLocales
                uncensored = episodePlatform.uncensored
                releaseDateTime = episodePlatform.releaseDateTime
            }
            .applyFlush()
            .toModel()
}
