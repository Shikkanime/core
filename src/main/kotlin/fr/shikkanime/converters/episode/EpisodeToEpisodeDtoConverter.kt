package fr.shikkanime.converters.episode

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.Episode
import fr.shikkanime.services.caches.LanguageCacheService
import fr.shikkanime.utils.withUTC
import java.time.format.DateTimeFormatter

class EpisodeToEpisodeDtoConverter : AbstractConverter<Episode, EpisodeDto>() {
    @Inject
    private lateinit var languageCacheService: LanguageCacheService

    override fun convert(from: Episode): EpisodeDto {
        val status = if (
            from.image.isNullOrBlank() ||
            from.description.isNullOrBlank() ||
            from.description?.startsWith("(") == true ||
            languageCacheService.detectLanguage(from.description) != from.anime!!.countryCode!!.name.lowercase() ||
            from.url?.contains("media-", true) == true
        ) Status.INVALID else Status.VALID

        return EpisodeDto(
            uuid = from.uuid,
            platform = convert(from.platform!!, PlatformDto::class.java),
            anime = convert(from.anime, AnimeDto::class.java),
            episodeType = from.episodeType!!,
            langType = from.langType!!,
            hash = from.hash!!,
            releaseDateTime = from.releaseDateTime.withUTC().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            season = from.season!!,
            number = from.number!!,
            title = from.title,
            url = from.url!!,
            image = from.image!!,
            duration = from.duration,
            description = from.description,
            uncensored = from.image!!.contains("nc/", true),
            lastUpdateDateTime = from.lastUpdateDateTime?.withUTC()?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            status = status,
        )
    }
}