package fr.shikkanime.converters.anime

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.jais.AnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.AnimeService
import java.time.ZonedDateTime

class JaisAnimeToAnimeConverter : AbstractConverter<AnimeDto, Anime>() {
    @Inject
    private lateinit var animeService: AnimeService

    override fun convert(from: AnimeDto): Anime {
        val findByName = animeService.findByName(CountryCode.FR, from.name)

        if (findByName.isNotEmpty()) {
            return findByName.first()
        }

        return Anime(
            countryCode = CountryCode.from(from.country.tag),
            name = from.name,
            releaseDateTime = ZonedDateTime.parse(from.releaseDate),
            image = from.image,
            description = from.description,
        )
    }
}