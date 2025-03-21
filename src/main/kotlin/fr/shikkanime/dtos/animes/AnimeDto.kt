package fr.shikkanime.dtos.animes

import fr.shikkanime.dtos.AnimePlatformDto
import fr.shikkanime.dtos.SeasonDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import java.util.*

data class AnimeDto(
    val uuid: UUID?,
    val countryCode: CountryCode,
    var name: String,
    var shortName: String,
    var slug: String,
    var releaseDateTime: String,
    val lastReleaseDateTime: String,
    val lastUpdateDateTime: String?,
    val image: String,
    val banner: String,
    val description: String?,
    val simulcasts: Set<SimulcastDto>?,
    var audioLocales: Set<String>? = null,
    var langTypes: Set<LangType>? = null,
    var seasons: Set<SeasonDto>? = null,
    var platformIds: Set<AnimePlatformDto>? = null,
)