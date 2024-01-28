package fr.shikkanime.dtos.animes

import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.enums.CountryCode
import java.util.*

open class AnimeDto(
    open val uuid: UUID?,
    open val countryCode: CountryCode,
    open var name: String,
    open var shortName: String,
    open var releaseDateTime: String,
    open val image: String? = null,
    open val banner: String? = null,
    open val description: String?,
    open val simulcasts: List<SimulcastDto>?,
    open val status: Status? = null,
    open val slug: String? = null,
    open val lastReleaseDateTime: String? = null,
    open val genres: List<String>? = null
)
