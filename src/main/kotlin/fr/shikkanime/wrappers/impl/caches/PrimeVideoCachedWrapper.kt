package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.wrappers.factories.AbstractPrimeVideoWrapper
import fr.shikkanime.wrappers.impl.PrimeVideoWrapper

object PrimeVideoCachedWrapper : AbstractPrimeVideoWrapper() {
    override suspend fun getShow(locale: String, id: String) = MapCache.getOrComputeAsync(
        "PrimeVideoCachedWrapper.getShow",
        typeToken = object : TypeToken<MapCacheValue<Show>>() {},
        key = locale to id
    ) { PrimeVideoWrapper.getShow(it.first, it.second) }

    override suspend fun getEpisodesByShowId(
        countryCode: CountryCode,
        id: String
    ) = MapCache.getOrComputeAsync(
        "PrimeVideoCachedWrapper.getEpisodesByShowId",
        typeToken = object : TypeToken<MapCacheValue<Array<Episode>>>() {},
        key = countryCode to id
    ) { PrimeVideoWrapper.getEpisodesByShowId(it.first, it.second) }
}