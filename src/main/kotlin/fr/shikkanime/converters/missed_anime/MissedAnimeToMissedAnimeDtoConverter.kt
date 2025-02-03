package fr.shikkanime.converters.missed_anime

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.animes.MissedAnimeDto
import fr.shikkanime.entities.MissedAnime

class MissedAnimeToMissedAnimeDtoConverter : AbstractConverter<MissedAnime, MissedAnimeDto>() {
    @Converter
    fun convert(from: MissedAnime): MissedAnimeDto {
        return MissedAnimeDto(
            anime = convert(from.anime, AnimeDto::class.java),
            episodeMissed = from.episodeMissed,
        )
    }
}