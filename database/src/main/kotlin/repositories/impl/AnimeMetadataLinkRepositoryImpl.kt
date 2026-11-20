package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.entities.AnimeMetadataLinkEntity
import fr.shikkanime.database.entities.AnimeMetadataLinkTable
import fr.shikkanime.database.repositories.AnimeMetadataLinkRepository
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.models.AnimeMetadataLink
import fr.shikkanime.models.MetadataPlatform
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [AnimeMetadataLinkRepository::class])
internal class AnimeMetadataLinkRepositoryImpl(
    private val animeRepository: AnimeRepository
) : AnimeMetadataLinkRepository() {

    /**
     * Finds one metadata link of an anime by its anime, platform and platform identifier.
     *
     * @param animeId identifier of the anime owning the link.
     * @param platform metadata platform of the link.
     * @param platformId identifier of the entry on the metadata platform.
     * @return the matching entity, or `null` when the link is not known yet.
     * @see [AnimeMetadataLink]
     */
    private fun findByAnimeIdPlatformAndPlatformId(
        animeId: Uuid,
        platform: MetadataPlatform,
        platformId: String
    ): AnimeMetadataLinkEntity? =
        AnimeMetadataLinkEntity.find {
            (AnimeMetadataLinkTable.anime eq animeId) and
                (AnimeMetadataLinkTable.platform eq platform) and
                (AnimeMetadataLinkTable.platformId eq platformId)
        }
            .limit(1)
            .firstOrNull()

    override fun findAllModels(): List<AnimeMetadataLink> =
        AnimeMetadataLinkEntity.all()
            .map(AnimeMetadataLinkEntity::toModel)

    override fun findAllByAnimeIds(animeIds: List<Uuid>): Map<Uuid?, List<AnimeMetadataLink>> =
        AnimeMetadataLinkEntity.find { AnimeMetadataLinkTable.anime inList animeIds }
            .map(AnimeMetadataLinkEntity::toModel)
            .groupBy(AnimeMetadataLink::animeId)

    override fun findByPlatformAndPlatformId(
        platform: MetadataPlatform,
        platformId: String
    ): AnimeMetadataLink? =
        AnimeMetadataLinkEntity.find {
            (AnimeMetadataLinkTable.platform eq platform) and
                (AnimeMetadataLinkTable.platformId eq platformId)
        }
            .limit(1)
            .firstOrNull()
            ?.toModel()

    override fun save(link: AnimeMetadataLink): AnimeMetadataLink =
        findByAnimeIdPlatformAndPlatformId(
            requireNotNull(link.animeId),
            link.platform,
            link.platformId
        )
            .newIfNotExists(link.id) {
                // The row belongs to the anime its metadata link already references
                this.anime = animeRepository.getReference(requireNotNull(link.animeId))
                platform = link.platform
                platformId = link.platformId
            }
            .applyFlush()
            .toModel()
}
