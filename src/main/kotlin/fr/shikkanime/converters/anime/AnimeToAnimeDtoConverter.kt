package fr.shikkanime.converters.anime

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.SeasonDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.mappings.EpisodeMappingWithoutAnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.SimulcastService.Companion.sortBySeasonAndYear
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.withUTCString
import org.hibernate.Hibernate

class AnimeToAnimeDtoConverter : AbstractConverter<Anime, AnimeDto>() {
    @Inject
    private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    override fun convert(from: Anime): AnimeDto {
        val (audioLocales, seasons) = episodeVariantCacheService.findAudioLocalesAndSeasonsByAnimeCache(from)!!

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
            audioLocales = audioLocales,
            langTypes = audioLocales.map { LangType.fromAudioLocale(from.countryCode, it) }.distinct().sorted(),
            seasons = seasons.map { (season, lastReleaseDateTime) -> SeasonDto(season, lastReleaseDateTime.withUTCString()) },
            episodes = if (Hibernate.isInitialized(from.mappings))
                convert(
                    from.mappings.sortedBy { it.releaseDateTime },
                    EpisodeMappingWithoutAnimeDto::class.java
                )?.toList()
            else
                null,
            status = from.status,
        )
    }
}