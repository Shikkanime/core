package fr.shikkanime.converters.anime

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.animes.AnimeNoStatusDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.Anime
import fr.shikkanime.services.caches.LanguageCacheService

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


        return AnimeDto.from(
            convert(from, AnimeNoStatusDto::class.java),
            status
        )
    }
}