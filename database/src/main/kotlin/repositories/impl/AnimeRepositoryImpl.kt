package fr.shikkanime.database.repositories.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.entities.AnimeEntity
import fr.shikkanime.database.entities.AnimePlatformEntity
import fr.shikkanime.database.entities.AnimePlatformTable
import fr.shikkanime.database.entities.AnimeTable
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.models.Anime
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single(binds = [AnimeRepository::class])
internal class AnimeRepositoryImpl(
    private val imageRepository: ImageRepository
) : AnimeRepository() {

    override fun findAllModels(): List<Anime> {
        val animes = super.findAll().toList()
        val images = imageRepository.findAllByEntityIds(animes.map { it.id.value })

        return animes.map { it.toModel(images[it.id.value].orEmpty()) }
    }

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

    override fun findByPlatformAndPlatformId(platform: Platform, platformId: String): Anime? =
        AnimeEntity.wrapRows(
            AnimeTable.innerJoin(AnimePlatformTable)
                .selectAll()
                .where {
                    (AnimePlatformTable.platform eq platform) and
                        (AnimePlatformTable.platformId eq platformId)
                }
                .limit(1)
        )
            .firstOrNull()
            // The shared image table has no relation to walk, the repository resolves the artwork.
            ?.let { it.toModel(imageRepository.findAllByEntityId(it.id.value)) }

    /**
     * Persists the anime row and returns it without its children.
     *
     * The caller upserts the children right after and composes the returned aggregate from those
     * fresh rows: composing them here would query what the caller immediately replaces.
     *
     * @param anime anime to persist.
     * @return the stored anime row, with empty collections.
     */
    override fun save(anime: Anime): Anime =
        anime.id?.let(::findById)
            .ifExists {
                updatedAt = TimeUtils.now()
            }
            .newIfNotExists(anime.id) { }
            .applyFlush()
            .toModel()
}
