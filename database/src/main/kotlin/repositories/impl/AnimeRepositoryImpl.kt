package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.entities.AnimeEntity
import fr.shikkanime.database.entities.AnimePlatformTable
import fr.shikkanime.database.entities.AnimeTable
import fr.shikkanime.database.repositories.AnimeLocalizationRepository
import fr.shikkanime.database.repositories.AnimePlatformRepository
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single(binds = [AnimeRepository::class])
internal class AnimeRepositoryImpl(
    private val imageRepository: ImageRepository,
    // The children are injected lazily: they reference this repository to mint their foreign keys
    private val localizationRepository: Lazy<AnimeLocalizationRepository>,
    private val platformRepository: Lazy<AnimePlatformRepository>
) : AnimeRepository() {

    override fun findAllModels(): List<AnimeWithChildren> =
        compose(
        super.findAll()
            .toList()
    )

    override fun findAllByNormalizedTitles(titles: List<String>): Map<String, AnimeWithChildren> =
        compose(
            AnimeEntity.find { AnimeTable.normalizedTitle inList titles }
                .toList()
        )
            .associateBy { it.anime.normalizedTitle }

    /**
     * Composes the aggregate view of the given rows from one batch read per relation.
     *
     * The image table is shared by every entity and cannot be reached by a relation, and the
     * relatable children are loaded as whole groups too: one lookup per row is the banned N+1.
     *
     * @param animes entity rows to compose.
     * @return the aggregates of the given rows, children included.
     * @see [AnimeWithChildren]
     */
    private fun compose(animes: List<AnimeEntity>): List<AnimeWithChildren> {
        val ids = animes.map { it.id.value }

        // Resolve the artwork of the whole page in one grouped read
        val images = imageRepository.findAllByEntityIds(ids)

        // Resolve the localized texts of the whole page in one grouped read
        val localizations = localizationRepository.value.findAllByAnimeIds(ids)

        // Resolve the platform source links of the whole page in one grouped read
        val platforms = platformRepository.value.findAllByAnimeIds(ids)

        return animes.map {
            AnimeWithChildren(
                anime = it.toModel(),
                images = images[it.id.value].orEmpty(),
                localizations = localizations[it.id.value].orEmpty(),
                platforms = platforms[it.id.value].orEmpty()
            )
        }
    }

    /**
     * Composes the aggregate view of one row from one batch read per relation.
     *
     * The children cannot be reached from the row alone: the shared artwork table and the
     * relationless children are resolved here, one whole group at a time.
     *
     * @param entity entity row to compose.
     * @return the aggregate of the given row, children included.
     * @see [AnimeWithChildren]
     */
    private fun composeOne(entity: AnimeEntity): AnimeWithChildren =
        compose(listOf(entity))
            .single()

    override fun findModelById(id: Uuid): AnimeWithChildren? =
        findById(id)
            ?.let(::composeOne)

    override fun findByNormalizedTitle(normalizedTitle: String): AnimeWithChildren? =
        AnimeEntity.find { AnimeTable.normalizedTitle eq normalizedTitle }
            .limit(1)
            .firstOrNull()
            ?.let(::composeOne)

    override fun findByPlatformAndPlatformId(platform: Platform, platformId: String): AnimeWithChildren? =
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
            ?.let(::composeOne)

    override fun save(anime: AnimeWithChildren): Anime =
        anime.anime.id?.let(::findById)
            .newIfNotExists(anime.anime.id) {
                normalizedTitle = anime.anime.normalizedTitle
            }
            .applyFlush()
            .toModel()
}
