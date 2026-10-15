package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.entities.AnimePlatformEntity
import fr.shikkanime.database.entities.AnimePlatformTable
import fr.shikkanime.database.repositories.AnimePlatformRepository
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [AnimePlatformRepository::class])
internal class AnimePlatformRepositoryImpl(
    private val animeRepository: AnimeRepository
) : AnimePlatformRepository() {

    /**
     * Finds one platform source link of an anime by its platform and platform identifier.
     *
     * @param animeId identifier of the anime owning the link.
     * @param platform streaming platform of the link.
     * @param platformId identifier of the anime on the streaming platform.
     * @return the matching entity, or `null` when the link is not known yet.
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

    override fun save(anime: Anime, animePlatform: AnimePlatform): AnimePlatform =
        findByIdPlatformAndPlatformId(anime.id, animePlatform.platform, animePlatform.platformId)
            .newIfNotExists(animePlatform.id) {
                this.anime = animeRepository.getReference(anime.id)
                platform = animePlatform.platform
                platformId = animePlatform.platformId
            }
            .applyFlush()
            .toModel()
}
