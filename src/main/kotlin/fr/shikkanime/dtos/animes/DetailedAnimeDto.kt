package fr.shikkanime.dtos.animes

import fr.shikkanime.dtos.SeasonDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingWithoutAnimeDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import java.util.*

data class DetailedAnimeDto(
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
    var audioLocales: List<String>? = null,
    var langTypes: List<LangType>? = null,
    var seasons: List<SeasonDto> = emptyList(),
    var episodes: List<EpisodeMappingWithoutAnimeDto>? = null,
    var status: Status? = null,
) {
    constructor(animeDto: AnimeDto) : this(
        animeDto.uuid,
        animeDto.countryCode,
        animeDto.name,
        animeDto.shortName,
        animeDto.slug,
        animeDto.releaseDateTime,
        animeDto.lastReleaseDateTime,
        animeDto.image,
        animeDto.banner,
        animeDto.description,
        animeDto.simulcasts,
    )
}