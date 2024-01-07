package fr.shikkanime.converters.anime

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.Anime
import org.apache.tika.language.detect.LanguageDetector
import org.hibernate.Hibernate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AnimeToAnimeDtoConverter : AbstractConverter<Anime, AnimeDto>() {
    private val languageDetector: LanguageDetector = LanguageDetector.getDefaultLanguageDetector().loadModels()
    private val utcZone = ZoneId.of("UTC")

    override fun convert(from: Anime): AnimeDto {
        val status = if (
            from.image.isNullOrBlank() ||
            from.description.isNullOrBlank() ||
            from.description?.startsWith("(") == true ||
            languageDetector.detect(from.description).language.lowercase() != from.countryCode!!.name.lowercase()
        ) Status.INVALID else Status.VALID

        return AnimeDto(
            uuid = from.uuid,
            releaseDateTime = from.releaseDateTime.withZoneSameInstant(utcZone)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            image = from.image,
            countryCode = from.countryCode!!,
            name = from.name!!,
            description = from.description,
            simulcasts = if (Hibernate.isInitialized(from.simulcasts)) convert(
                from.simulcasts,
                SimulcastDto::class.java
            ) else null,
            status = status,
        )
    }
}