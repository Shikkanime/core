package fr.shikkanime.converters.anime

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.AnimePlatformDto
import fr.shikkanime.dtos.SeasonDto
import fr.shikkanime.dtos.SimulcastDto
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
            slug = from.slug!!,
            releaseDateTime = from.releaseDateTime.withUTCString(),
            lastReleaseDateTime = from.lastReleaseDateTime.withUTCString(),
            image = from.image!!,
            banner = from.banner!!,
            description = from.description,
            simulcasts = if (Hibernate.isInitialized(from.simulcasts))
                convert(
                    from.simulcasts.sortBySeasonAndYear(),
                    SimulcastDto::class.java
                )?.toList()
            else
                null,
            audioLocales = audioLocales.takeIf { it.isNotEmpty() },
            langTypes = audioLocales.map { LangType.fromAudioLocale(from.countryCode, it) }.distinct().sorted().takeIf { it.isNotEmpty() },
            seasons = seasons.map { (season, lastReleaseDateTime) -> SeasonDto(season, lastReleaseDateTime.withUTCString()) }.takeIf { it.isNotEmpty() },
            status = from.status,
            platformIds = if (Hibernate.isInitialized(from.platformIds))
                convert(from.platformIds, AnimePlatformDto::class.java)?.toList()
            else
                null
        )
    }
}