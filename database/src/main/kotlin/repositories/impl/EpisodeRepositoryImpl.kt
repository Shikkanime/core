package fr.shikkanime.database.repositories.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.core.pagination.Page
import fr.shikkanime.core.pagination.PageRequest
import fr.shikkanime.database.entities.EpisodeEntity
import fr.shikkanime.database.entities.EpisodeReleaseTable
import fr.shikkanime.database.entities.EpisodeTable
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.database.repositories.EpisodeLocalizationRepository
import fr.shikkanime.database.repositories.EpisodeReleaseRepository
import fr.shikkanime.database.repositories.EpisodeRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.exposed.pagination.paginate
import fr.shikkanime.models.Episode
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.EpisodeWithChildren
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single(binds = [EpisodeRepository::class])
internal class EpisodeRepositoryImpl(
    private val animeRepository: AnimeRepository,
    private val imageRepository: ImageRepository,
    // The children are injected lazily: they reference this repository to mint their foreign keys
    private val localizationRepository: Lazy<EpisodeLocalizationRepository>,
    private val releaseRepository: Lazy<EpisodeReleaseRepository>
) : EpisodeRepository() {

    /**
     * Composes the aggregate view of the given rows from one batch read per relation.
     *
     * The image table is shared by every entity and cannot be reached by a relation, and the
     * relatable children are loaded as whole groups too: one lookup per row is the banned N+1.
     *
     * @param episodes entity rows to compose.
     * @return the aggregates of the given rows, children included.
     * @see [EpisodeWithChildren]
     */
    private fun compose(episodes: List<EpisodeEntity>): List<EpisodeWithChildren> {
        // Nothing to compose: skip the relation reads instead of querying empty id lists
        if (episodes.isEmpty()) return emptyList()

        val ids = episodes.map { it.id.value }

        // Resolve the artwork of the whole page in one grouped read
        val images = imageRepository.findAllByEntityIds(ids)

        // Resolve the localized texts of the whole page in one grouped read
        val localizations = localizationRepository.value.findAllByEpisodeIds(ids)

        // Resolve the releases of the whole page in one grouped read
        val releases = releaseRepository.value.findAllByEpisodeIds(ids)

        return episodes.map {
            EpisodeWithChildren(
                episode = it.toModel(),
                images = images[it.id.value].orEmpty(),
                localizations = localizations[it.id.value].orEmpty(),
                releases = releases[it.id.value].orEmpty()
            )
        }
    }

    /**
     * Composes the aggregate view of one row from one batch read per relation.
     *
     * @param entity entity row to compose.
     * @return the aggregate of the given row, children included.
     * @see [EpisodeWithChildren]
     */
    private fun composeOne(entity: EpisodeEntity): EpisodeWithChildren =
        compose(listOf(entity))
            .single()

    /**
     * Tells whether a stored episode carries different metadata than the given one.
     *
     * @param stored episode as currently stored in database.
     * @param candidate episode to persist.
     * @return `true` when one metadata field of the episode changed.
     * @see [Episode]
     */
    private fun hasChanged(stored: EpisodeEntity, candidate: Episode): Boolean =
        stored.season != candidate.season
            || stored.episodeType != candidate.episodeType
            || stored.number != candidate.number
            || stored.releaseDateTime != candidate.releaseDateTime
            || stored.duration != candidate.duration

    override fun findAllByAnimeId(animeId: Uuid): List<EpisodeWithChildren> =
        compose(
            EpisodeEntity.find { EpisodeTable.anime eq animeId }
                .orderBy(
                    EpisodeTable.season to SortOrder.ASC,
                    EpisodeTable.episodeType to SortOrder.ASC,
                    EpisodeTable.number to SortOrder.ASC
                )
                .toList()
        )

    override fun findPageByAnimeId(animeId: Uuid, request: PageRequest): Page<EpisodeWithChildren> {
        val page = EpisodeEntity.find { EpisodeTable.anime eq animeId }
            .orderBy(
                EpisodeTable.season to SortOrder.ASC,
                EpisodeTable.episodeType to SortOrder.ASC,
                EpisodeTable.number to SortOrder.ASC
            )
            .paginate(request)

        return Page(compose(page.data), page.page, page.size, page.totalElements)
    }

    override fun findPageByReleaseDate(request: PageRequest): Page<EpisodeWithChildren> {
        val page = EpisodeEntity.all()
            .orderBy(
                EpisodeTable.releaseDateTime to SortOrder.DESC,
                EpisodeTable.id to SortOrder.ASC
            )
            .paginate(request)

        return Page(compose(page.data), page.page, page.size, page.totalElements)
    }

    override fun findModelById(id: Uuid): EpisodeWithChildren? =
        findById(id)
            ?.let(::composeOne)

    override fun findByAnimeIdSeasonEpisodeTypeAndNumber(
        animeId: Uuid,
        season: Int,
        episodeType: EpisodeType,
        number: Int
    ): EpisodeWithChildren? =
        EpisodeEntity.find {
            (EpisodeTable.anime eq animeId) and
                (EpisodeTable.season eq season) and
                (EpisodeTable.episodeType eq episodeType) and
                (EpisodeTable.number eq number)
        }
            .limit(1)
            .firstOrNull()
            ?.let(::composeOne)

    override fun findByPlatformAndPlatformId(
        platform: Platform,
        platformId: String
    ): EpisodeWithChildren? =
        EpisodeEntity.wrapRows(
            EpisodeTable.innerJoin(EpisodeReleaseTable)
                .select(EpisodeTable.columns)
                .where {
                    (EpisodeReleaseTable.platform eq platform) and
                        (EpisodeReleaseTable.platformId eq platformId)
                }
                .limit(1)
        )
            .firstOrNull()
            ?.let(::composeOne)

    override fun save(episode: Episode): Episode =
        episode.id?.let(::findById)
            .ifExists {
                if (hasChanged(this, episode)) {
                    season = episode.season
                    episodeType = episode.episodeType
                    number = episode.number
                    releaseDateTime = episode.releaseDateTime
                    duration = episode.duration
                    updatedAt = TimeUtils.now()
                }
            }
            .newIfNotExists(episode.id) {
                anime = animeRepository.getReference(requireNotNull(episode.animeId))
                season = episode.season
                episodeType = episode.episodeType
                number = episode.number
                releaseDateTime = episode.releaseDateTime
                duration = episode.duration
            }
            .applyFlush()
            .toModel()
}
