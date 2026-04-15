package fr.shikkanime.services

import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Season
import fr.shikkanime.repositories.AnalyticsRepository

class AnalyticsService : AbstractService<EpisodeMapping, AnalyticsRepository>() {
    fun getAllMarketShare(startYear: Int, endYear: Int) =
        repository.getAllMarketShare(startYear, endYear)
            .sortedWith(compareBy({ it.simulcast.year }, { Season.entries.indexOf(it.simulcast.season) }))

    fun getSubCoverage(countryCode: CountryCode, startYear: Int, endYear: Int) =
        repository.getSubCoverage(countryCode, startYear, endYear)
            .sortedWith(compareBy({ it.simulcast.year }, { Season.entries.indexOf(it.simulcast.season) }))

    fun getAllGenreCoverage(startYear: Int, endYear: Int) =
        repository.getAllGenreCoverage(startYear, endYear)
            .sortedWith(compareBy({ it.simulcast.year }, { Season.entries.indexOf(it.simulcast.season) }))
}