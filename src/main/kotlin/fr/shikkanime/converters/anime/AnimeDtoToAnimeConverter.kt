package fr.shikkanime.converters.anime

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.AnimeService
import java.time.ZonedDateTime

class AnimeDtoToAnimeConverter : AbstractConverter<AnimeDto, Anime>() {
    @Inject
    private lateinit var animeService: AnimeService

    override fun convert(from: AnimeDto): Anime {
        val findByUuid = animeService.find(from.uuid)

        if (findByUuid != null)
            return findByUuid

        val findByName = animeService.findAllByLikeName(CountryCode.FR, from.name)

        if (findByName.isNotEmpty())
            return findByName.first()

        return Anime(
            countryCode = from.countryCode,
            name = from.name,
            releaseDateTime = ZonedDateTime.parse(from.releaseDateTime),
            image = from.image,
            banner = from.banner,
            description = from.description,
            simulcasts = convert(from.simulcasts ?: emptyList(), Simulcast::class.java).toMutableSet()
        )
    }
}