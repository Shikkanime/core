package fr.shikkanime.dtos.animes

import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.enums.CountryCode
import java.util.*

open class AnimeDto(
    open val uuid: UUID?,
    open val countryCode: CountryCode,
    open var name: String,
    open var shortName: String,
    open var slug: String? = null,
    open var releaseDateTime: String,
    open val lastReleaseDateTime: String? = null,
    open val image: String? = null,
    open val banner: String? = null,
    open val description: String?,
    open val simulcasts: List<SimulcastDto>?,
)
