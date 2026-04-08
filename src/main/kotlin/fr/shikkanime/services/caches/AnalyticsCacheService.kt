package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.dtos.analytics.GenreCoverageDto
import fr.shikkanime.dtos.analytics.MarketShareDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.factories.impl.GenreCoverageFactory
import fr.shikkanime.factories.impl.MarketShareFactory
import fr.shikkanime.services.AnalyticsService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue

class AnalyticsCacheService : ICacheService {
    @Inject
    private lateinit var analyticsService: AnalyticsService
    @Inject
    private lateinit var marketShareFactory: MarketShareFactory
    @Inject
    private lateinit var genreCoverageFactory: GenreCoverageFactory

    fun getAllMarketShare(startYear: Int, endYear: Int) = MapCache.getOrCompute(
        "AnalyticsCacheService.getAllMarketShare",
        classes = listOf(
            Anime::class.java,
            EpisodeMapping::class.java,
            EpisodeVariant::class.java,
            Simulcast::class.java
        ),
        typeToken = object : TypeToken<MapCacheValue<Array<MarketShareDto>>>() {},
        key = startYear to endYear
    ) { (startYear, endYear) ->
        analyticsService.getAllMarketShare(startYear, endYear).map(marketShareFactory::toDto).toTypedArray()
    }

    fun getSubCoverage(countryCode: CountryCode, startYear: Int, endYear: Int) = MapCache.getOrCompute(
        "AnalyticsCacheService.getSubCoverage",
        classes = listOf(
            Anime::class.java,
            EpisodeMapping::class.java,
            EpisodeVariant::class.java,
            Simulcast::class.java
        ),
        typeToken = object : TypeToken<MapCacheValue<Array<MarketShareDto>>>() {},
        key = Triple(countryCode, startYear, endYear)
    ) { (countryCode, startYear, endYear) ->
        analyticsService.getSubCoverage(countryCode, startYear, endYear).map(marketShareFactory::toDto).toTypedArray()
    }

    fun getAllGenreCoverage(startYear: Int, endYear: Int) = MapCache.getOrCompute(
        "AnalyticsCacheService.getAllGenreCoverage",
        classes = listOf(Anime::class.java, Genre::class.java, Simulcast::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<GenreCoverageDto>>>() {},
        key = startYear to endYear
    ) { (startYear, endYear) ->
        analyticsService.getAllGenreCoverage(startYear, endYear).map(genreCoverageFactory::toDto).toTypedArray()
    }
}