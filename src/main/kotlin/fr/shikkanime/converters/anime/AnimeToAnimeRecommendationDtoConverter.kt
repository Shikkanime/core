package fr.shikkanime.converters.anime

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.animes.AnimeRecommendationDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.services.RecommendationService
import fr.shikkanime.services.caches.AnimeCacheService
import jakarta.inject.Inject

class AnimeToAnimeRecommendationDtoConverter : AbstractConverter<Anime, AnimeRecommendationDto>() {
    @Inject
    private lateinit var animeCacheService: AnimeCacheService

    override fun convert(from: Anime): AnimeRecommendationDto {
        val animeDto = convert(from, AnimeDto::class.java)
        val animes = animeCacheService.findAllWithGenres()!!
        val recommended =
            if (from.genres.isNotEmpty()) RecommendationService.getRecommendations(animes, from) else emptySet()

        return AnimeRecommendationDto.fromAnimeDto(animeDto, recommended.map { convert(it.first, AnimeDto::class.java) }.take(6))
    }
}