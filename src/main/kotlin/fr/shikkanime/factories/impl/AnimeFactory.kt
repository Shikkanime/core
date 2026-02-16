package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.factories.IGenericFactory
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.services.SimulcastService.Companion.sortBySeasonAndYear
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.AnimePlatformCacheService
import fr.shikkanime.services.caches.AnimeTagCacheService
import fr.shikkanime.services.caches.GenreCacheService
import fr.shikkanime.services.seo.JsonLdBuilder
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.toTreeSet
import fr.shikkanime.utils.withUTCString
import org.hibernate.Hibernate

class AnimeFactory : IGenericFactory<Anime, AnimeDto> {
    @Inject private lateinit var animeCacheService: AnimeCacheService
    @Inject private lateinit var animePlatformCacheService: AnimePlatformCacheService
    @Inject private lateinit var simulcastFactory: SimulcastFactory
    @Inject private lateinit var genreCacheService: GenreCacheService
    @Inject private lateinit var animeTagCacheService: AnimeTagCacheService
    @Inject private lateinit var jsonLdBuilder: JsonLdBuilder
    @Inject private lateinit var attachmentService: AttachmentService

    override fun toDto(entity: Anime) = toDto(entity, false)

    fun toDto(entity: Anime, showAllPlatforms: Boolean): AnimeDto {
        val entityUuid = entity.uuid!!
        val audioLocales = animeCacheService.getAudioLocales(entityUuid)
        val langTypes = animeCacheService.getLangTypes(entity).toSet()
        val seasons = animeCacheService.findAllSeasons(entity).toSet()
        val genres = genreCacheService.findAllByAnime(entityUuid)
        val tags = animeTagCacheService.findAllByAnime(entityUuid)
        val carouselAttachment = attachmentService.findByEntityUuidTypeAndActive(entityUuid, ImageType.CAROUSEL)?.url

        val platforms = animePlatformCacheService.findAllByAnime(entity)
            .filter { showAllPlatforms || it.platform.isStreaming }
            .toSet()

        return AnimeDto(
            uuid = entityUuid,
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
            genres = genres.toSet(),
            tags = tags.toSet(),
            langTypes = langTypes,
            seasons = seasons,
            platformIds = platforms.toTreeSet(),
            hasCarousel = !carouselAttachment.isNullOrBlank(),
        ).also { dto -> dto.jsonLd = jsonLdBuilder.build(dto) }
    }
}