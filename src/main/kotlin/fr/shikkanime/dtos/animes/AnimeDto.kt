package fr.shikkanime.dtos.animes

import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.enums.CountryCode
import java.util.*

data class AnimeDto(
    val uuid: UUID?,
    val countryCode: CountryCode,
    var name: String,
    var shortName: String,
    var slug: String? = null,
    var releaseDateTime: String,
    val lastReleaseDateTime: String? = null,
    val image: String? = null,
    val banner: String? = null,
    val description: String?,
    val simulcasts: List<SimulcastDto>?,
)
