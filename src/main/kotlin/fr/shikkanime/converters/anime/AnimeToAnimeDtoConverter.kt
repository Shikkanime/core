package fr.shikkanime.converters.anime

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.SimulcastService.Companion.sortBySeasonAndYear
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.withUTCString

class AnimeToAnimeDtoConverter : AbstractConverter<Anime, AnimeDto>() {
    @Inject
    private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

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
            langTypes = audioLocales.map { LangType.fromAudioLocale(from.countryCode, it) }.sorted(),
            status = from.status,
        )
    }
}