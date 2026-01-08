package fr.shikkanime.dtos.animes

import fr.shikkanime.dtos.AnimePlatformDto
import fr.shikkanime.dtos.GenreDto
import fr.shikkanime.dtos.SeasonDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import java.io.Serializable
import java.util.*

data class AnimeDto(
    val uuid: UUID?,
    val countryCode: CountryCode,
    var name: String,
    var shortName: String,
    var slug: String,
    var releaseDateTime: String,
    val lastReleaseDateTime: String,
    val lastUpdateDateTime: String,
    val description: String?,
    val simulcasts: Set<SimulcastDto>?,
    val audioLocales: Set<String>?,
    var genres: Set<GenreDto>? = null,
    var tags: Set<AnimeTagDto>? = null,
    var langTypes: Set<LangType>? = null,
    var seasons: Set<SeasonDto>? = null,
    var platformIds: Set<AnimePlatformDto>? = null,
    var thumbnail: String? = null,
    var banner: String? = null,
    var carousel: String? = null,
    var title: String? = null,
    var jsonLd: String? = null,
) : Serializable