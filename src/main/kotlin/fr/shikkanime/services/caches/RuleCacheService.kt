package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Rule
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.RuleService
import fr.shikkanime.utils.MapCache

class RuleCacheService : AbstractCacheService {
    @Inject
    private lateinit var ruleService: RuleService

    fun findAll() = MapCache.getOrCompute(
        "RuleCacheService.findAll",
        classes = listOf(Rule::class.java),
        key = "all",
    ) { ruleService.findAll() }

    fun findAllByPlatformSeriesIdAndSeasonId(platform: Platform, seriesId: String, seasonId: String) =
        findAll().filter { it.platform == platform && it.seriesId == seriesId && it.seasonId == seasonId }
}