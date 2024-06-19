package fr.shikkanime.caches

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType

data class CountryCodeSlugSeasonEpisodeTypeNumberKeyCache(
    val countryCode: CountryCode,
    val slug: String,
    val season: Int,
    val episodeType: EpisodeType,
    val number: Int
)
