package fr.shikkanime.caches

import fr.shikkanime.entities.enums.Season

data class SeasonYearKeyCache(
    val season: Season,
    val year: Int
)
