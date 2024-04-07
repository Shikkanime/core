package fr.shikkanime.dtos

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Status
import java.util.*

data class AnimeDto(
    val uuid: UUID?,
    val countryCode: CountryCode,
    var name: String,
    var shortName: String,
    var releaseDateTime: String,
    val image: String,
    val banner: String?,
    val description: String?,
    val simulcasts: List<SimulcastDto>,
    val slug: String,
    val lastReleaseDateTime: String,
    val status: Status,
    val langTypes: List<LangType>,
)