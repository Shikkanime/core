package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.factories.IGenericFactory
import fr.shikkanime.services.SimulcastService.Companion.sortBySeasonAndYear
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.AnimePlatformCacheService
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.toTreeSet
import fr.shikkanime.utils.withUTCString
import org.hibernate.Hibernate

class AnimeFactory : IGenericFactory<Anime, AnimeDto> {
    @Inject private lateinit var animeCacheService: AnimeCacheService
    @Inject private lateinit var animePlatformCacheService: AnimePlatformCacheService
    @Inject private lateinit var simulcastFactory: SimulcastFactory

    override fun toDto(entity: Anime) = toDto(entity, false)

    fun toDto(entity: Anime, showAllPlatforms: Boolean): AnimeDto {
        val audioLocales = animeCacheService.getAudioLocales(entity.uuid!!)
        val langTypes = animeCacheService.getLangTypes(entity).toSet()
        val seasons = animeCacheService.findAllSeasons(entity)
        val platforms = animePlatformCacheService.findAllByAnime(entity)

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
            simulcasts = if (Hibernate.isInitialized(entity.simulcasts)) entity.simulcasts.sortBySeasonAndYear().map(simulcastFactory::toDto).toSet() else null,
            audioLocales = audioLocales.toTreeSet(),
            langTypes = langTypes,
            seasons = seasons.toSet(),
            platformIds = platforms.filter { showAllPlatforms || it.platform.isStreaming }.toTreeSet()
        )
    }
}