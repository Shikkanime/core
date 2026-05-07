package fr.shikkanime.builders

import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.withUTCString
import java.time.ZonedDateTime
import java.util.*

class AnimeDtoBuilder {
    var uuid: UUID? = null
    lateinit var countryCode: CountryCode
    lateinit var name: String
    var shortName: String? = null
    var slug: String? = null
    var releaseDateTime: String? = null
    var lastReleaseDateTime: String? = null
    var lastUpdateDateTime: String? = null
    var description: String? = null
    var simulcasts: Set<SimulcastDto>? = null
    var audioLocales: Set<String>? = null

    fun build(): AnimeDto {
        val defaultDefaultDateTime = releaseDateTime ?: ZonedDateTime.now().withUTCString()

        return AnimeDto(
            uuid = uuid,
            countryCode = countryCode,
            name = name,
            shortName = shortName ?: name,
            slug = slug ?: name,
            releaseDateTime = defaultDefaultDateTime,
            lastReleaseDateTime = lastReleaseDateTime ?: defaultDefaultDateTime,
            lastUpdateDateTime = lastUpdateDateTime ?: defaultDefaultDateTime,
            description = description,
            simulcasts = simulcasts,
            audioLocales = audioLocales
        )
    }
}