package fr.shikkanime.database.entities

import fr.shikkanime.models.ModelConvertible
import fr.shikkanime.models.Season
import fr.shikkanime.models.Simulcast
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

internal const val SIMULCAST_TABLE_NAME = "simulcast"
internal const val SIMULCAST_TABLE_ID = SIMULCAST_TABLE_NAME + ID

internal object SimulcastTable : ShikkTable(SIMULCAST_TABLE_NAME) {
    val season = enumerationByName<Season>("season", 10)
    val year = integer("year")

    init {
        uniqueIndex("season_year$UQ", season, year)
    }
}

internal class SimulcastEntity(id: EntityID<Uuid>) :
    ShikkEntity(id, SimulcastTable),
    ModelConvertible<Simulcast> {
    companion object : UuidEntityClass<SimulcastEntity>(SimulcastTable)

    var season by SimulcastTable.season
    var year by SimulcastTable.year

    /**
     * Maps this row to the simulcast domain model.
     *
     * @return the [Simulcast] representation of this row.
     * @see [Simulcast]
     */
    override fun toModel(): Simulcast =
        Simulcast(
            id = id.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            season = season,
            year = year
        )
}
