package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.entities.AnimePlatformEntity
import fr.shikkanime.database.entities.AnimePlatformTable
import fr.shikkanime.database.repositories.AnimePlatformRepository
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [AnimePlatformRepository::class])
internal class AnimePlatformRepositoryImpl(
    private val animeRepository: AnimeRepository
) : AnimePlatformRepository() {

    override fun findAllByAnimeIds(animeIds: List<Uuid>): Map<Uuid?, List<AnimePlatform>> =
        AnimePlatformEntity.find { AnimePlatformTable.anime inList animeIds }
            .map(AnimePlatformEntity::toModel)
            .groupBy(AnimePlatform::animeId)

    /**
     * Finds one platform source link of an anime by its platform and platform identifier.
     *
     * @param animeId identifier of the anime owning the link.
     * @param platform streaming platform of the link.
     * @param platformId identifier of the anime on the streaming platform.
     * @return the matching entity, or `null` when the link is not known yet.
     * @see [AnimePlatform]
     */
    private fun findByIdPlatformAndPlatformId(
        animeId: Uuid,
        platform: Platform,
        platformId: String
    ): AnimePlatformEntity? =
        AnimePlatformEntity.find {
            (AnimePlatformTable.anime eq animeId) and
                (AnimePlatformTable.platform eq platform) and
                (AnimePlatformTable.platformId eq platformId)
        }
            .limit(1)
            .firstOrNull()

    override fun save(animePlatform: AnimePlatform): AnimePlatform =
        findByIdPlatformAndPlatformId(
            requireNotNull(animePlatform.animeId),
            animePlatform.platform,
            animePlatform.platformId
        )
            .newIfNotExists(animePlatform.id) {
                // The row belongs to the anime its source link already references
                this.anime = animeRepository.getReference(requireNotNull(animePlatform.animeId))
                platform = animePlatform.platform
                platformId = animePlatform.platformId
            }
            .applyFlush()
            .toModel()
}
