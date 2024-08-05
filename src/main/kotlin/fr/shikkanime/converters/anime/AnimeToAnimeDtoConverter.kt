package fr.shikkanime.converters.anime

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.services.SimulcastService.Companion.sortBySeasonAndYear
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.withUTCString
import org.hibernate.Hibernate

class AnimeToAnimeDtoConverter : AbstractConverter<Anime, AnimeDto>() {
    override fun convert(from: Anime): AnimeDto {
        return AnimeDto(
            uuid = from.uuid,
            countryCode = from.countryCode!!,
            name = from.name!!,
            shortName = StringUtils.getShortName(from.name!!),
            slug = from.slug,
            releaseDateTime = from.releaseDateTime.withUTCString(),
            lastReleaseDateTime = from.lastReleaseDateTime.withUTCString(),
            image = from.image,
            banner = from.banner,
            description = from.description,
            simulcasts = if (Hibernate.isInitialized(from.simulcasts))
                convert(
                    from.simulcasts.sortBySeasonAndYear(),
                    SimulcastDto::class.java
                )?.toList()
            else
                null,
        )
    }
}