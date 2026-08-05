package fr.shikkanime.database.entities

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

const val ANIME_TABLE_NAME = "anime"
const val ANIME_TABLE_ID = ANIME_TABLE_NAME + ID

object AnimeTable : ShikkTable(ANIME_TABLE_NAME)

class AnimeEntity(id: EntityID<Uuid>) : ShikkEntity(id, AnimeTable) {
    companion object : UuidEntityClass<AnimeEntity>(AnimeTable)

    var simulcasts by SimulcastEntity via JoinAnimeSimulcastTable
}