package fr.shikkanime.dtos.animes

import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.enums.CountryCode
import java.util.*

data class AnimeDto(
    override val uuid: UUID?,
    override val countryCode: CountryCode,
    override var name: String,
    override var shortName: String,
    override var releaseDateTime: String,
    override val image: String? = null,
    override val banner: String? = null,
    override val description: String?,
    override val simulcasts: List<SimulcastDto>?,
    override val slug: String? = null,
    override val lastReleaseDateTime: String? = null,
    val status: Status? = null,
) : AnimeNoStatusDto(
    uuid,
    countryCode,
    name,
    shortName,
    releaseDateTime,
    image,
    banner,
    description,
    simulcasts,
    slug,
    lastReleaseDateTime
) {
    companion object {
        fun from(animeNoStatusDto: AnimeNoStatusDto, status: Status?): AnimeDto {
            return AnimeDto(
                animeNoStatusDto.uuid,
                animeNoStatusDto.countryCode,
                animeNoStatusDto.name,
                animeNoStatusDto.shortName,
                animeNoStatusDto.releaseDateTime,
                animeNoStatusDto.image,
                animeNoStatusDto.banner,
                animeNoStatusDto.description,
                animeNoStatusDto.simulcasts,
                animeNoStatusDto.slug,
                animeNoStatusDto.lastReleaseDateTime,
                status,
            )
        }
    }
}
