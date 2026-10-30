package fr.shikkanime.database.entities

import fr.shikkanime.models.EpisodeLocalization
import fr.shikkanime.models.Locale
import fr.shikkanime.models.ModelConvertible
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

const val EPISODE_LOCALIZATION_TABLE_NAME = "episode_localization"

internal object EpisodeLocalizationTable : ShikkTable(EPISODE_LOCALIZATION_TABLE_NAME) {
    val episode = reference(
        EPISODE_TABLE_ID,
        EpisodeTable,
        fkName = EPISODE_LOCALIZATION_TABLE_NAME + "_" + EPISODE_TABLE_NAME + FK
    )
    val locale = enumerationByName<Locale>("locale", 10)
    val title = varchar("title", 1000)
        .nullable()
    val description = text("description")
        .nullable()

    init {
        uniqueIndex("${EPISODE_TABLE_ID}_locale$UQ", episode, locale)
    }
}

internal class EpisodeLocalizationEntity(id: EntityID<Uuid>) :
    ShikkEntity(id, EpisodeLocalizationTable),
    ModelConvertible<EpisodeLocalization> {
    companion object : UuidEntityClass<EpisodeLocalizationEntity>(EpisodeLocalizationTable)

    var episode by EpisodeLocalizationTable.episode
    var locale by EpisodeLocalizationTable.locale
    var title by EpisodeLocalizationTable.title
    var description by EpisodeLocalizationTable.description

    override fun toModel(): EpisodeLocalization =
        EpisodeLocalization(
            id = id.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            episodeId = episode.value,
            locale = locale,
            title = title,
            description = description
        )
}
