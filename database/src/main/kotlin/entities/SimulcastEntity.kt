package fr.shikkanime.database.entities

import fr.shikkanime.models.Season
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

const val SIMULCAST_TABLE_NAME = "simulcast"
const val SIMULCAST_TABLE_ID = SIMULCAST_TABLE_NAME + ID

object SimulcastTable : ShikkTable(SIMULCAST_TABLE_NAME) {
    val season = enumerationByName<Season>("season", 10)
    val year = integer("year")

    init {
        uniqueIndex("season_year$UQ", season, year)
    }
}

class SimulcastEntity(id: EntityID<Uuid>) : ShikkEntity(id, SimulcastTable) {
    companion object : UuidEntityClass<SimulcastEntity>(SimulcastTable)

    var season by SimulcastTable.season
    var year by SimulcastTable.year
}