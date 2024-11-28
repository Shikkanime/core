package fr.shikkanime.dtos.animes

import fr.shikkanime.dtos.AnimePlatformDto
import fr.shikkanime.dtos.SeasonDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingWithoutAnimeDto
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
    val description: String? = null,
    val simulcasts: List<SimulcastDto>? = null,
    var audioLocales: Set<String>? = null,
    var langTypes: List<LangType>? = null,
    var seasons: List<SeasonDto>? = null,
    var episodes: List<EpisodeMappingWithoutAnimeDto>? = null,
    var status: Status? = null,
    var platformIds: List<AnimePlatformDto>? = null,
)