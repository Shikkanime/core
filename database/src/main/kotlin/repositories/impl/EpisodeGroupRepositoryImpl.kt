package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.entities.AnimeTable
import fr.shikkanime.database.entities.EpisodeReleaseEntity
import fr.shikkanime.database.entities.EpisodeReleaseTable
import fr.shikkanime.database.entities.EpisodeTable
import fr.shikkanime.database.repositories.EpisodeGroupRepository
import fr.shikkanime.models.EpisodeObservation
import fr.shikkanime.models.EpisodeRelease
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.Locale
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.select
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [EpisodeGroupRepository::class])
internal class EpisodeGroupRepositoryImpl : EpisodeGroupRepository {

    override fun findObservations(
        from: LocalDateTime,
        to: LocalDateTime?,
        animeId: Uuid?,
        episodeType: EpisodeType?
    ): List<EpisodeObservation> =
        EpisodeReleaseTable
            .innerJoin(EpisodeTable)
            .innerJoin(AnimeTable)
            .select(
                AnimeTable.id,
                AnimeTable.normalizedTitle,
                EpisodeTable.anime,
                EpisodeTable.id,
                EpisodeTable.season,
                EpisodeTable.episodeType,
                EpisodeTable.number,
                EpisodeReleaseTable.id,
                EpisodeReleaseTable.audioLocale,
                EpisodeReleaseTable.releaseDateTime
            )
            .where {
                (EpisodeReleaseTable.releaseDateTime greaterEq from) and
                    (to?.let { EpisodeReleaseTable.releaseDateTime lessEq it } ?: Op.TRUE) and
                    (animeId?.let { EpisodeTable.anime eq it } ?: Op.TRUE) and
                    (episodeType?.let { EpisodeTable.episodeType eq it } ?: Op.TRUE)
            }
            .map { row ->
                EpisodeObservation(
                    animeId = row[EpisodeTable.anime].value,
                    animeSlug = row[AnimeTable.normalizedTitle],
                    episodeId = row[EpisodeTable.id].value,
                    season = row[EpisodeTable.season],
                    episodeType = row[EpisodeTable.episodeType],
                    number = row[EpisodeTable.number],
                    releaseId = row[EpisodeReleaseTable.id].value,
                    audioLocale = row[EpisodeReleaseTable.audioLocale],
                    releaseDateTime = row[EpisodeReleaseTable.releaseDateTime]
                )
            }

    override fun findAudioLocalesByEpisodeIds(episodeIds: List<Uuid>): Map<Uuid, Set<Locale>> {
        if (episodeIds.isEmpty())
            return emptyMap()

        return EpisodeReleaseTable
            .select(EpisodeReleaseTable.episode, EpisodeReleaseTable.audioLocale)
            .where { EpisodeReleaseTable.episode inList episodeIds }
            .groupBy(
                { row -> row[EpisodeReleaseTable.episode].value },
                { row -> row[EpisodeReleaseTable.audioLocale] }
            )
            .mapValues { (_, locales) -> locales.toSet() }
    }

    override fun findReleasesByIds(ids: Collection<Uuid>): Map<Uuid, EpisodeRelease> {
        if (ids.isEmpty())
            return emptyMap()

        return EpisodeReleaseEntity
            .find { EpisodeReleaseTable.id inList ids.map { EntityID(it, EpisodeReleaseTable) } }
            .associate { it.id.value to it.toModel() }
    }
}
