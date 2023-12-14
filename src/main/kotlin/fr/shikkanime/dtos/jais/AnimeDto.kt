package fr.shikkanime.dtos.jais

import java.io.Serializable
import java.util.*

data class AnimeDto(
    val uuid: UUID,
    val country: CountryDto,
    val name: String,
    val releaseDate: String,
    val image: String,
    val description: String,
    val hashes: List<String>,
    val simulcasts: List<SimulcastDto>,
) : Serializable