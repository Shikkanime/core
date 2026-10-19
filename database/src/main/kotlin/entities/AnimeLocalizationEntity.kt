package fr.shikkanime.database.entities

import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.Locale
import fr.shikkanime.models.ModelConvertible
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

const val ANIME_LOCALIZATION_TABLE_NAME = "anime_localization"

internal object AnimeLocalizationTable : ShikkTable(ANIME_LOCALIZATION_TABLE_NAME) {
    val anime = reference(
        ANIME_TABLE_ID,
        AnimeTable,
        fkName = ANIME_LOCALIZATION_TABLE_NAME + "_" + ANIME_TABLE_NAME + FK
    )
    val locale = enumerationByName<Locale>("locale", 10)
    val title = varchar("title", 255)
    val description = text("description")
        .nullable()

    init {
        uniqueIndex("${ANIME_TABLE_ID}_locale$UQ", anime, locale)
    }
}

internal class AnimeLocalizationEntity(id: EntityID<Uuid>) :
    ShikkEntity(id, AnimeLocalizationTable),
    ModelConvertible<AnimeLocalization> {
    companion object : UuidEntityClass<AnimeLocalizationEntity>(AnimeLocalizationTable)

    var anime by AnimeLocalizationTable.anime
    var locale by AnimeLocalizationTable.locale
    var title by AnimeLocalizationTable.title
    var description by AnimeLocalizationTable.description

    override fun toModel(): AnimeLocalization =
        AnimeLocalization(
            id = id.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            animeId = anime.value,
            locale = locale,
            title = title,
            description = description
        )
}
