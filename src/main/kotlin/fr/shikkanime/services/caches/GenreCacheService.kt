package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.dtos.GenreDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Genre
import fr.shikkanime.factories.impl.GenreFactory
import fr.shikkanime.services.GenreService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import java.util.*

class GenreCacheService : ICacheService {
    @Inject private lateinit var genreService: GenreService
    @Inject private lateinit var genreFactory: GenreFactory

    fun findAllByAnime(animeUuid: UUID) = MapCache.getOrCompute(
        "GenreCacheService.findAllByAnime",
        classes = listOf(Anime::class.java, Genre::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<GenreDto>>>() {},
        key = animeUuid,
    ) { genreService.findAllByAnime(it).map(genreFactory::toDto).toTypedArray() }
}