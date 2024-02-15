package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.caches.CountryCodeNamePaginationKeyCache
import fr.shikkanime.caches.CountryCodeUUIDSortPaginationKeyCache
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.AnimeService
import fr.shikkanime.utils.MapCache
import java.util.*

class AnimeCacheService : AbstractCacheService {
    @Inject
    private lateinit var animeService: AnimeService

    private val findAllByCache =
        MapCache<CountryCodeUUIDSortPaginationKeyCache, PageableDto<AnimeDto>>(classes = listOf(Anime::class.java)) {
            PageableDto.fromPageable(
                animeService.findAllBy(it.countryCode, it.uuid, it.sort, it.page, it.limit),
                AnimeDto::class.java
            )
        }

    private val findAllByNameCache = MapCache<CountryCodeNamePaginationKeyCache, PageableDto<AnimeDto>>(classes = listOf(Anime::class.java)) {
        PageableDto.fromPageable(
            animeService.findAllByName(it.name, it.countryCode, it.page, it.limit),
            AnimeDto::class.java
        )
    }

    private val findBySlugCache = MapCache<String, AnimeDto>(classes = listOf(Anime::class.java)) {
        AbstractConverter.convert(animeService.findBySlug(it), AnimeDto::class.java)
    }

    fun findAllBy(
        countryCode: CountryCode?,
        uuid: UUID?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int
    ) = findAllByCache[CountryCodeUUIDSortPaginationKeyCache(countryCode, uuid, sort, page, limit)]

    fun findAllByName(name: String, countryCode: CountryCode?, page: Int, limit: Int) =
        findAllByNameCache[CountryCodeNamePaginationKeyCache(countryCode, name, page, limit)]

    fun findBySlug(slug: String) = findBySlugCache[slug]
}