package fr.shikkanime.database.entities

import fr.shikkanime.models.Episode
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.ModelConvertible
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.jetbrains.exposed.v1.datetime.datetime
import kotlin.uuid.Uuid

const val EPISODE_TABLE_NAME = "episode"
const val EPISODE_TABLE_ID = EPISODE_TABLE_NAME + ID

internal object EpisodeTable : ShikkTable(EPISODE_TABLE_NAME) {
    val anime = reference(
        ANIME_TABLE_ID,
        AnimeTable,
        fkName = EPISODE_TABLE_NAME + "_" + ANIME_TABLE_NAME + FK
    )
    val season = integer("season")
    val number = integer("number")
    val episodeType = enumerationByName<EpisodeType>("episode_type", 20)
    val releaseDateTime = datetime("release_date_time")
    val duration = long("duration")

    init {
        // An episode is identified by its anime, its season, its type and its number: a release
        // harvested from two platforms lands on the same row.
        uniqueIndex(
            "${ANIME_TABLE_ID}_season_episode_type_number$UQ",
            anime,
            season,
            episodeType,
            number
        )
    }
}

internal class EpisodeEntity(id: EntityID<Uuid>) :
    ShikkEntity(id, EpisodeTable),
    ModelConvertible<Episode> {
    companion object : UuidEntityClass<EpisodeEntity>(EpisodeTable)

    var anime by EpisodeTable.anime
    var season by EpisodeTable.season
    var number by EpisodeTable.number
    var episodeType by EpisodeTable.episodeType
    var releaseDateTime by EpisodeTable.releaseDateTime
    var duration by EpisodeTable.duration

    /**
     * Maps this row to the flat episode identity.
     *
     * The rows owned by an episode are never walked here: localizations, platforms and images are
     * composed into [fr.shikkanime.models.EpisodeWithChildren] by the repository, with one batch
     * read per relation, so this conversion stays a total function of this row.
     *
     * @return the [Episode] representation of this row.
     * @see [Episode]
     */
    override fun toModel(): Episode =
        Episode(
            id = id.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            animeId = anime.value,
            season = season,
            number = number,
            episodeType = episodeType,
            releaseDateTime = releaseDateTime,
            duration = duration
        )
}
