package fr.shikkanime.dtos

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import java.util.*

data class AnimeDto(
    val uuid: UUID?,
    val countryCode: CountryCode,
    var name: String,
    var shortName: String,
    val slug: String? = null,
    var releaseDateTime: String,
    val lastReleaseDateTime: String? = null,
    val image: String? = null,
    val banner: String? = null,
    val description: String?,
    val simulcasts: List<SimulcastDto>?,
    val audioLocales: List<String>? = null,
    val langTypes: List<LangType>? = null,
    val status: Status? = null,
)
