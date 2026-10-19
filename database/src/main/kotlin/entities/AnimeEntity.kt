package fr.shikkanime.database.entities

import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.ModelConvertible
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

const val ANIME_TABLE_NAME = "anime"
const val ANIME_TABLE_ID = ANIME_TABLE_NAME + ID

internal object AnimeTable : ShikkTable(ANIME_TABLE_NAME) {
    val normalizedTitle = varchar("normalized_title", 255)
        .uniqueIndex()
}

internal class AnimeEntity(id: EntityID<Uuid>) :
    ShikkEntity(id, AnimeTable),
    ModelConvertible<Anime> {
    companion object : UuidEntityClass<AnimeEntity>(AnimeTable)

    var normalizedTitle by AnimeTable.normalizedTitle

    /**
     * Maps this row to the flat anime identity.
     *
     * The rows owned by an anime are never walked here: localizations, platforms and images are
     * composed into [AnimeWithChildren] by the repository, with one batch read per relation, so
     * this conversion stays a total function of this row.
     *
     * @return the [Anime] representation of this row.
     * @see [AnimeWithChildren]
     */
    override fun toModel(): Anime =
        Anime(
            id = id.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            normalizedTitle = normalizedTitle
        )
}
