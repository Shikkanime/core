package fr.shikkanime.dtos.animes

import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.enums.CountryCode
import java.util.*

data class AnimeRecommendationDto(
    @Transient
    override val uuid: UUID?,
    @Transient
    override val countryCode: CountryCode,
    @Transient
    override var name: String,
    @Transient
    override var shortName: String,
    @Transient
    override var releaseDateTime: String,
    @Transient
    override val image: String? = null,
    @Transient
    override val banner: String? = null,
    @Transient
    override val description: String?,
    @Transient
    override val simulcasts: List<SimulcastDto>?,
    @Transient
    override val status: Status? = null,
    @Transient
    override val slug: String? = null,
    @Transient
    override val lastReleaseDateTime: String? = null,
    @Transient
    override val genres: List<String>?,
    val recommendations: List<AnimeDto>? = null
) : AnimeDto(
    uuid,
    countryCode,
    name,
    shortName,
    releaseDateTime,
    image,
    banner,
    description,
    simulcasts,
    status,
    slug,
    lastReleaseDateTime,
    genres
) {
    companion object {
        fun fromAnimeDto(animeDto: AnimeDto, recommendations: List<AnimeDto>? = null): AnimeRecommendationDto {
            return AnimeRecommendationDto(
                animeDto.uuid,
                animeDto.countryCode,
                animeDto.name,
                animeDto.shortName,
                animeDto.releaseDateTime,
                animeDto.image,
                animeDto.banner,
                animeDto.description,
                animeDto.simulcasts,
                animeDto.status,
                animeDto.slug,
                animeDto.lastReleaseDateTime,
                animeDto.genres,
                recommendations
            )
        }
    }
}
