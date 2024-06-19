package fr.shikkanime.converters.anime

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.SeasonDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.mappings.EpisodeMappingWithoutAnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.services.SimulcastService.Companion.sortBySeasonAndYear
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.withUTCString
import org.hibernate.Hibernate
import java.time.ZonedDateTime

class AnimeToAnimeDtoConverter : AbstractConverter<Anime, AnimeDto>() {
    @Inject
    private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    override fun convert(from: Anime): AnimeDto {
        val audioLocales = episodeVariantCacheService.findAllAudioLocalesByAnime(from)!!

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
            simulcasts = convert(
                from.simulcasts.sortBySeasonAndYear(),
                SimulcastDto::class.java
            )?.toList(),
            audioLocales = audioLocales,
            langTypes = audioLocales.map { LangType.fromAudioLocale(from.countryCode, it) }.distinct().sorted(),
            seasons = episodeMappingService.findAllSeasonsByAnime(from)
                .map { SeasonDto(it[0] as Int, (it[1] as ZonedDateTime).withUTCString()) },
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