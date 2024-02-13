package fr.shikkanime.dtos.animes

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.simulcasts.SimulcastDto
import fr.shikkanime.entities.enums.CountryCode
import java.io.Serializable
import java.util.*

data class UpdatedAnimeDto(
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
    val lastReleaseDateTime: String? = null
) : Serializable
