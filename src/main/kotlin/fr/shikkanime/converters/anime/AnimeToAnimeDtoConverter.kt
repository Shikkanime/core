package fr.shikkanime.converters.anime

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.AnimePlatformDto
import fr.shikkanime.dtos.SeasonDto
import fr.shikkanime.dtos.simulcasts.SimulcastDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.SimulcastService.Companion.sortBySeasonAndYear
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.AnimePlatformCacheService
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.withUTCString
import org.hibernate.Hibernate

class AnimeToAnimeDtoConverter : AbstractConverter<Anime, AnimeDto>() {
    @Inject
    private lateinit var animeCacheService: AnimeCacheService

    @Inject
    private lateinit var animePlatformCacheService: AnimePlatformCacheService

    @Converter
    fun convert(from: Anime): AnimeDto {
        val (audioLocales, seasons) = animeCacheService.findAudioLocalesAndSeasonsByAnimeCache(from)
            ?: Pair(emptySet(), sortedMapOf())

        return AnimeDto(
            uuid = from.uuid,
            countryCode = from.countryCode!!,
            name = from.name!!,
            shortName = StringUtils.getShortName(from.name!!),
            slug = from.slug!!,
            releaseDateTime = from.releaseDateTime.withUTCString(),
            lastReleaseDateTime = from.lastReleaseDateTime.withUTCString(),
            lastUpdateDateTime = from.lastUpdateDateTime?.withUTCString(),
            image = from.image!!,
            banner = from.banner!!,
            description = from.description,
            simulcasts = if (Hibernate.isInitialized(from.simulcasts)) convert(
                from.simulcasts.sortBySeasonAndYear(),
                SimulcastDto::class.java
            )!! else null,
            audioLocales = audioLocales.takeIf { it.isNotEmpty() },
            langTypes = audioLocales.map { LangType.fromAudioLocale(from.countryCode, it) }.distinct().sorted().takeIf { it.isNotEmpty() }?.toSet(),
            seasons = seasons.map { (season, lastReleaseDateTime) -> SeasonDto(season, lastReleaseDateTime.withUTCString()) }.takeIf { it.isNotEmpty() }?.toSet(),
            status = from.status,
            platformIds = convert(animePlatformCacheService.findAllByAnime(from).sortedBy { it.platform?.name }.toSet(), AnimePlatformDto::class.java)
        )
    }
}