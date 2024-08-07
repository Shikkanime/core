package fr.shikkanime.converters.anime

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.animes.DetailedAnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.services.AnimeService
import java.time.ZonedDateTime

class AnimeDtoToAnimeConverter : AbstractConverter<DetailedAnimeDto, Anime>() {
    @Inject
    private lateinit var animeService: AnimeService

    override fun convert(from: DetailedAnimeDto): Anime {
        val findByUuid = animeService.find(from.uuid)

        if (findByUuid != null)
            return findByUuid

        val findByName = animeService.findByName(from.countryCode, from.name)

        if (findByName != null)
            return findByName

        return Anime(
            countryCode = from.countryCode,
            name = from.name,
            releaseDateTime = ZonedDateTime.parse(from.releaseDateTime),
            image = from.image,
            banner = from.banner,
            description = from.description,
            simulcasts = convert(from.simulcasts ?: emptyList(), Simulcast::class.java)!!.toMutableSet(),
            slug = from.slug,
        )
    }
}