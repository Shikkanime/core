package fr.shikkanime.converters.anime

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.Anime
import fr.shikkanime.services.SimulcastService.Companion.sortBySeasonAndYear
import fr.shikkanime.services.caches.LanguageCacheService
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.withUTC
import org.hibernate.Hibernate
import java.time.format.DateTimeFormatter

class AnimeToAnimeDtoConverter : AbstractConverter<Anime, AnimeDto>() {
    @Inject
    private lateinit var languageCacheService: LanguageCacheService

    override fun convert(from: Anime): AnimeDto {
        val status = if (
            from.image.isNullOrBlank() ||
            from.banner.isNullOrBlank() ||
            from.description.isNullOrBlank() ||
            from.description?.startsWith("(") == true ||
            languageCacheService.detectLanguage(from.description) != from.countryCode!!.name.lowercase()
        ) Status.INVALID else Status.VALID

        return AnimeDto(
            uuid = from.uuid,
            releaseDateTime = from.releaseDateTime.withUTC()
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            image = from.image,
            banner = from.banner,
            countryCode = from.countryCode!!,
            name = from.name!!,
            shortName = StringUtils.getShortName(from.name!!),
            description = from.description,
            simulcasts = if (Hibernate.isInitialized(from.simulcasts)) convert(
                from.simulcasts.sortBySeasonAndYear(),
                SimulcastDto::class.java
            ) else null,
            status = status,
            slug = from.slug,
            lastReleaseDateTime = from.lastReleaseDateTime.withUTC()
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        )
    }
}