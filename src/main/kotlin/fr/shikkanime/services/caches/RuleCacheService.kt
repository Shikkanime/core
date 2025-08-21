package fr.shikkanime.services.caches

import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import fr.shikkanime.entities.Rule
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.RuleService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.MapCacheValue
import fr.shikkanime.utils.StringUtils

class RuleCacheService : ICacheService {
    @Inject private lateinit var ruleService: RuleService

    fun findAll() = MapCache.getOrCompute(
        "RuleCacheService.findAll",
        classes = listOf(Rule::class.java),
        typeToken = object : TypeToken<MapCacheValue<Array<Rule>>>() {},
        key = StringUtils.EMPTY_STRING,
    ) { ruleService.findAll().toTypedArray() }

    fun findAllByPlatformSeriesIdAndSeasonId(platform: Platform, seriesId: String, seasonId: String) =
        findAll().filter { it.platform == platform && it.seriesId == seriesId && it.seasonId == seasonId }
}