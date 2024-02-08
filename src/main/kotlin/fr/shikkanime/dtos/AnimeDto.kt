package fr.shikkanime.dtos

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.enums.CountryCode
import java.io.Serializable
import java.util.*

data class AnimeDto(
    val uuid: UUID?,
    val countryCode: CountryCode,
    var name: String,
    var shortName: String,
    var releaseDateTime: String,
    val image: String? = null,
    val banner: String? = null,
    val description: String?,
    val simulcasts: List<SimulcastDto>?,
    val status: Status? = null,
    val slug: String? = null,
) : Serializable
