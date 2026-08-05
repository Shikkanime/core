package fr.shikkanime.database.entities

import org.jetbrains.exposed.v1.core.Table

const val JOIN_ANIME_SIMULCAST_TABLE_NAME = "join_${ANIME_TABLE_NAME}_${SIMULCAST_TABLE_NAME}"

object JoinAnimeSimulcastTable : Table(JOIN_ANIME_SIMULCAST_TABLE_NAME) {
    val anime = reference(ANIME_TABLE_ID, AnimeTable, fkName = JOIN_ANIME_SIMULCAST_TABLE_NAME + "_" + ANIME_TABLE_NAME + FK)
    val simulcast = reference(SIMULCAST_TABLE_ID, SimulcastTable, fkName = JOIN_ANIME_SIMULCAST_TABLE_NAME + "_" + SIMULCAST_TABLE_NAME + FK)

    override val primaryKey = PrimaryKey(anime, simulcast, name = JOIN_ANIME_SIMULCAST_TABLE_NAME + PK)
}