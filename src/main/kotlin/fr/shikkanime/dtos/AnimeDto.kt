package fr.shikkanime.dtos

import fr.shikkanime.entities.enums.CountryCode
import java.io.Serializable
import java.util.*

data class AnimeDto(
    val uuid: UUID?,
    val countryCode: CountryCode,
    val name: String,
    val releaseDateTime: String,
    val image: String? = null,
    val description: String?,
    val simulcasts: List<SimulcastDto>?,
) : Serializable
