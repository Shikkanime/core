package fr.shikkanime.dtos.animes

import fr.shikkanime.dtos.SeasonDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingWithoutAnimeDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import java.util.*

data class DetailedAnimeDto(
    @Transient
    override val uuid: UUID?,
    @Transient
    override val countryCode: CountryCode,
    @Transient
    override var name: String,
    @Transient
    override var shortName: String,
    @Transient
    override var slug: String? = null,
    @Transient
    override var releaseDateTime: String,
    @Transient
    override val lastReleaseDateTime: String? = null,
    @Transient
    override val image: String? = null,
    @Transient
    override val banner: String? = null,
    @Transient
    override val description: String?,
    @Transient
    override val simulcasts: List<SimulcastDto>?,
    var audioLocales: List<String>? = null,
    var langTypes: List<LangType>? = null,
    var seasons: List<SeasonDto> = emptyList(),
    var episodes: List<EpisodeMappingWithoutAnimeDto>? = null,
    var status: Status? = null,
) : AnimeDto(
    uuid, countryCode, name, shortName, slug, releaseDateTime, lastReleaseDateTime, image, banner, description, simulcasts
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