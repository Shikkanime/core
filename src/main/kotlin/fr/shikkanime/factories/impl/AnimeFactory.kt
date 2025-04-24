package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.SeasonDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.factories.IGenericFactory
import fr.shikkanime.services.SimulcastService.Companion.sortBySeasonAndYear
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.AnimePlatformCacheService
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.withUTCString
import org.hibernate.Hibernate

class AnimeFactory : IGenericFactory<Anime, AnimeDto> {
    @Inject
    private lateinit var animeCacheService: AnimeCacheService

    @Inject
    private lateinit var animePlatformCacheService: AnimePlatformCacheService

    @Inject
    private lateinit var simulcastFactory: SimulcastFactory

    @Inject
    private lateinit var animePlatformFactory: AnimePlatformFactory

    override fun toDto(entity: Anime): AnimeDto {
        val audioLocales = animeCacheService.getAudioLocales(entity) ?: emptySet()
        val seasons = animeCacheService.getSeasons(entity) ?: emptyMap()
        val platforms = animePlatformCacheService.getAll(entity)

        return AnimeDto(
            uuid = entity.uuid,
            countryCode = entity.countryCode!!,
            name = entity.name!!,
            shortName = StringUtils.getShortName(entity.name!!),
            slug = entity.slug!!,
            releaseDateTime = entity.releaseDateTime.withUTCString(),
            lastReleaseDateTime = entity.lastReleaseDateTime.withUTCString(),
            lastUpdateDateTime = entity.lastUpdateDateTime.withUTCString(),
            description = entity.description,
            simulcasts = if (Hibernate.isInitialized(entity.simulcasts)) entity.simulcasts.sortBySeasonAndYear().map { simulcastFactory.toDto(it) }.toSet() else null,
            audioLocales = audioLocales.takeIf { it.isNotEmpty() },
            langTypes = audioLocales.map { LangType.fromAudioLocale(entity.countryCode, it) }.distinct().sorted().takeIf { it.isNotEmpty() }?.toSet(),
            seasons = seasons.map { (season, lastReleaseDateTime) -> SeasonDto(season, lastReleaseDateTime.withUTCString()) }.takeIf { it.isNotEmpty() }?.toSet(),
            platformIds = platforms.sortedBy { it.platform?.name }.map { animePlatformFactory.toDto(it) }.toSet()
        )
    }
}