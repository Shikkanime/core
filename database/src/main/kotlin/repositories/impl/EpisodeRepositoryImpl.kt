package fr.shikkanime.database.repositories.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.entities.EpisodeEntity
import fr.shikkanime.database.entities.EpisodePlatformTable
import fr.shikkanime.database.entities.EpisodeTable
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.database.repositories.EpisodeLocalizationRepository
import fr.shikkanime.database.repositories.EpisodePlatformRepository
import fr.shikkanime.database.repositories.EpisodeRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.models.Episode
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.EpisodeWithChildren
import fr.shikkanime.models.Platform
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single(binds = [EpisodeRepository::class])
internal class EpisodeRepositoryImpl(
    private val animeRepository: AnimeRepository,
    private val imageRepository: ImageRepository,
    private val localizationRepository: EpisodeLocalizationRepository,
    private val platformRepository: EpisodePlatformRepository
) : EpisodeRepository() {

    override fun findAllByAnimeId(animeId: Uuid): List<EpisodeWithChildren> =
        compose(
            EpisodeEntity.find { EpisodeTable.anime eq animeId }
                .orderBy(
                    EpisodeTable.season to SortOrder.ASC,
                    EpisodeTable.number to SortOrder.ASC
                )
                .toList()
        )

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
        val ids = episodes.map { it.id.value }

        // Resolve the artwork of the whole page in one grouped read
        val images = imageRepository.findAllByEntityIds(ids)

        // Resolve the localized texts of the whole page in one grouped read
        val localizations = localizationRepository.findAllByEpisodeIds(ids)

        // Resolve the platform releases of the whole page in one grouped read
        val platforms = platformRepository.findAllByEpisodeIds(ids)

        return episodes.map {
            EpisodeWithChildren(
                episode = it.toModel(),
                images = images[it.id.value].orEmpty(),
                localizations = localizations[it.id.value].orEmpty(),
                platforms = platforms[it.id.value].orEmpty()
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

    override fun findModelById(id: Uuid): EpisodeWithChildren? =
        findById(id)
            ?.let(::composeOne)

    override fun findByAnimeIdAndSeasonAndNumberAndEpisodeType(
        animeId: Uuid,
        season: Int,
        number: Int,
        episodeType: EpisodeType
    ): EpisodeWithChildren? =
        EpisodeEntity.find {
            (EpisodeTable.anime eq animeId) and
                (EpisodeTable.season eq season) and
                (EpisodeTable.number eq number) and
                (EpisodeTable.episodeType eq episodeType)
        }
            .limit(1)
            .firstOrNull()
            ?.let(::composeOne)

    override fun findByPlatformAndPlatformId(
        platform: Platform,
        platformId: String
    ): EpisodeWithChildren? =
        EpisodeEntity.wrapRows(
            EpisodeTable.innerJoin(EpisodePlatformTable)
                .selectAll()
                .where {
                    (EpisodePlatformTable.platform eq platform) and
                        (EpisodePlatformTable.platformId eq platformId)
                }
                .limit(1)
        )
            .firstOrNull()
            ?.let(::composeOne)

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
            || stored.number != candidate.number
            || stored.episodeType != candidate.episodeType
            || stored.releaseDateTime != candidate.releaseDateTime
            || stored.duration != candidate.duration

    override fun save(episode: EpisodeWithChildren): Episode =
        episode.episode.id?.let(::findById)
            .ifExists {
                if (hasChanged(this, episode.episode)) {
                    season = episode.episode.season
                    number = episode.episode.number
                    episodeType = episode.episode.episodeType
                    releaseDateTime = episode.episode.releaseDateTime
                    duration = episode.episode.duration
                    updatedAt = TimeUtils.now()
                }
            }
            .newIfNotExists(episode.episode.id) {
                anime = animeRepository.getReference(requireNotNull(episode.episode.animeId))
                season = episode.episode.season
                number = episode.episode.number
                episodeType = episode.episode.episodeType
                releaseDateTime = episode.episode.releaseDateTime
                duration = episode.episode.duration
            }
            .applyFlush()
            .toModel()
}
