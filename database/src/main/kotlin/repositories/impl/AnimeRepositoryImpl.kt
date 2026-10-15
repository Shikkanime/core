package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.entities.AnimeEntity
import fr.shikkanime.database.entities.AnimePlatformEntity
import fr.shikkanime.database.entities.AnimePlatformTable
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.models.Anime
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [AnimeRepository::class])
internal class AnimeRepositoryImpl(
    private val imageRepository: ImageRepository
) : AnimeRepository() {

    /**
     * Finds the anime aggregate with its artwork.
     *
     * The image table is shared by every entity and cannot be resolved by a foreign key, so the
     * images are composed here from [ImageRepository], while localizations and platforms come from
     * the lazy relations of [AnimeEntity].
     *
     * @param id identifier of the anime to load.
     * @return the matching anime, or `null` when it does not exist.
     */
    override fun findModelById(id: Uuid): Anime? =
        findById(id)?.toModel(imageRepository.findAllByEntityId(id))

    override fun findAll(): List<Anime> =
        AnimeEntity.all()
            .map { it.toModel(imageRepository.findAllByEntityId(it.id.value)) }

    override fun findByPlatformAndPlatformId(platform: Platform, platformId: String): Anime? =
        AnimePlatformEntity.find {
            (AnimePlatformTable.platform eq platform) and
                (AnimePlatformTable.platformId eq platformId)
        }
            .limit(1)
            .firstOrNull()
            ?.let { findById(it.anime.value) }
            ?.let { entity -> entity.toModel(imageRepository.findAllByEntityId(entity.id.value)) }

    override fun save(anime: Anime): Anime =
        findById(anime.id)
            .newIfNotExists(anime.id) { }
            .applyFlush()
            .toModel(imageRepository.findAllByEntityId(anime.id))
}
