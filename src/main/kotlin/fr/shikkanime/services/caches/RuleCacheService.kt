package fr.shikkanime.services.caches

import com.google.inject.Inject
import fr.shikkanime.entities.Rule
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.RuleService
import fr.shikkanime.utils.MapCache

class RuleCacheService : AbstractCacheService {
    companion object {
        private const val DEFAULT_ALL_KEY = "all"
    }

    @Inject
    private lateinit var ruleService: RuleService

    private val cache = MapCache(
        "RuleCacheService.cache",
        classes = listOf(Rule::class.java),
        fn = { listOf(DEFAULT_ALL_KEY) }
    ) {
        ruleService.findAll()
    }

    fun findAll() = cache[DEFAULT_ALL_KEY] ?: emptyList()

    fun findAllByPlatformSeriesIdAndSeasonId(platform: Platform, seriesId: String, seasonId: String) =
        findAll().filter {
            it.platform == platform && it.seriesId == seriesId && it.seasonId == seasonId
        }
}