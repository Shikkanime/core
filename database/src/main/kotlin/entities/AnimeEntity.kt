package fr.shikkanime.database.entities

import fr.shikkanime.models.Anime
import fr.shikkanime.models.Image
import fr.shikkanime.models.ModelConvertible
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

const val ANIME_TABLE_NAME = "anime"
const val ANIME_TABLE_ID = ANIME_TABLE_NAME + ID

internal object AnimeTable : ShikkTable(ANIME_TABLE_NAME)

internal class AnimeEntity(id: EntityID<Uuid>) : ShikkEntity(id, AnimeTable), ModelConvertible<Anime> {
    companion object : UuidEntityClass<AnimeEntity>(AnimeTable)

    val localizations by AnimeLocalizationEntity referrersOn AnimeLocalizationTable.anime
    val platforms by AnimePlatformEntity referrersOn AnimePlatformTable.anime

    /**
     * Maps this row to the domain aggregate without artwork.
     *
     * The image table is shared by every entity and cannot be resolved by a foreign key, so the
     * anime repository always calls [toModel] with the images loaded from its own repository.
     *
     * @return the [Anime] representation of this row, with no image.
     */
    override fun toModel(): Anime =
        toModel(emptyList())

    /**
     * Maps this row and its children to the domain aggregate.
     *
     * Localizations and platforms are read through their lazy relations. [images] is passed in
     * because the image table is shared by every entity and cannot be resolved by a foreign key;
     * the repository loads it with its own query.
     *
     * @param images artwork rows owned by this anime.
     * @return the [Anime] representation of this row.
     */
    fun toModel(images: List<Image>): Anime =
        Anime(
            id = id.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            images = images,
            localizations = localizations.map(AnimeLocalizationEntity::toModel),
            platforms = platforms.map(AnimePlatformEntity::toModel)
        )
}
