package fr.shikkanime.dtos.animes

import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import java.util.*

open class AnimeNoStatusDto(
    @Transient
    open val uuid: UUID?,
    @Transient
    open val countryCode: CountryCode,
    @Transient
    open var name: String,
    @Transient
    open var shortName: String,
    @Transient
    open var releaseDateTime: String,
    @Transient
    open val image: String? = null,
    @Transient
    open val banner: String? = null,
    @Transient
    open val description: String?,
    @Transient
    open val simulcasts: List<SimulcastDto>?,
    @Transient
    open val slug: String? = null,
    @Transient
    open val lastReleaseDateTime: String? = null,
    @Transient
    open val langTypes: List<LangType>? = null,
) {
    fun toAnimeDto() = AnimeDto.from(this, null)
}