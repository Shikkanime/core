package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.dtos.animes.AnimeTagDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimeTag
import fr.shikkanime.entities.Tag
import fr.shikkanime.factories.impl.AnimeTagFactory
import fr.shikkanime.services.AnimeTagService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import java.util.*

class AnimeTagCacheService : ICacheService {
    @Inject private lateinit var animeTagService: AnimeTagService
    @Inject private lateinit var animeTagFactory: AnimeTagFactory

    fun findAllByAnime(animeUuid: UUID) = MapCache.getOrCompute(
        "AnimeTagCacheService.findAllByAnime",
        classes = listOf(Anime::class.java, AnimeTag::class.java, Tag::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<AnimeTagDto>>>() {},
        key = animeUuid,
    ) { animeTagService.findAllByAnime(it).map(animeTagFactory::toDto).toTypedArray() }
}