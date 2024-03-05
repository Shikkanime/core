package fr.shikkanime.converters.episode

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.Episode
import fr.shikkanime.utils.withUTC
import org.apache.tika.language.detect.LanguageDetector
import java.time.format.DateTimeFormatter

class EpisodeToEpisodeDtoConverter : AbstractConverter<Episode, EpisodeDto>() {
    private val languageDetector: LanguageDetector = LanguageDetector.getDefaultLanguageDetector().loadModels()

    override fun convert(from: Episode): EpisodeDto {
        val status = if (
            from.image.isNullOrBlank() ||
            from.description.isNullOrBlank() ||
            from.description?.startsWith("(") == true ||
            languageDetector.detect(from.description).language.lowercase() != from.anime!!.countryCode!!.name.lowercase() ||
            from.url?.contains("media-", true) == true
        ) Status.INVALID else Status.VALID

        return EpisodeDto(
            uuid = from.uuid,
            platform = from.platform!!,
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