package fr.shikkanime.wrappers.impl.caches

import com.google.gson.reflect.TypeToken
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.wrappers.factories.AbstractPrimeVideoWrapper
import fr.shikkanime.wrappers.impl.PrimeVideoWrapper
import kotlinx.coroutines.runBlocking

object PrimeVideoCachedWrapper : AbstractPrimeVideoWrapper() {
    override suspend fun getEpisodesByShowId(
        countryCode: CountryCode,
        id: String
    ) = MapCache.getOrCompute(
        "PrimeVideoCachedWrapper.getEpisodesByShowId",
        typeToken = object : TypeToken<MapCacheValue<Array<Episode>>>() {},
        key = countryCode to id
    ) { runBlocking { PrimeVideoWrapper.getEpisodesByShowId(it.first, it.second) } }
}