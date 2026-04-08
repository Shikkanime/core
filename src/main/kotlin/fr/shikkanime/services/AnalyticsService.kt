package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Season
import fr.shikkanime.repositories.AnalyticsRepository

class AnalyticsService : AbstractService<EpisodeMapping, AnalyticsRepository>() {
    @Inject
    private lateinit var analyticsRepository: AnalyticsRepository

    override fun getRepository() = analyticsRepository

    fun getAllMarketShare(startYear: Int, endYear: Int) =
        analyticsRepository.getAllMarketShare(startYear, endYear)
            .sortedWith(compareBy({ it.simulcast.year }, { Season.entries.indexOf(it.simulcast.season) }))

    fun getSubCoverage(countryCode: CountryCode, startYear: Int, endYear: Int) =
        analyticsRepository.getSubCoverage(countryCode, startYear, endYear)
            .sortedWith(compareBy({ it.simulcast.year }, { Season.entries.indexOf(it.simulcast.season) }))

    fun getAllGenreCoverage(startYear: Int, endYear: Int) =
        analyticsRepository.getAllGenreCoverage(startYear, endYear)
            .sortedWith(compareBy({ it.simulcast.year }, { Season.entries.indexOf(it.simulcast.season) }))
}