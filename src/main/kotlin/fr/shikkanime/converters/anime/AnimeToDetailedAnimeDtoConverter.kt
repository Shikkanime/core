package fr.shikkanime.converters.anime

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.SeasonDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.animes.DetailedAnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.withUTCString

class AnimeToDetailedAnimeDtoConverter : AbstractConverter<Anime, DetailedAnimeDto>() {
    @Inject
    private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    override fun convert(from: Anime): DetailedAnimeDto {
        val (audioLocales, seasons) = episodeVariantCacheService.findAudioLocalesAndSeasonsByAnimeCache(from)!!

        return DetailedAnimeDto(convert(from, AnimeDto::class.java)).apply {
            this.audioLocales = audioLocales
            this.langTypes = audioLocales.map { LangType.fromAudioLocale(from.countryCode!!, it) }.distinct().sorted()
            this.seasons = seasons.map { (season, lastReleaseDateTime) -> SeasonDto(season, lastReleaseDateTime.withUTCString()) }
            this.status = from.status
        }
    }
}