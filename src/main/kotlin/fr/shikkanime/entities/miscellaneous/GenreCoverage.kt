package fr.shikkanime.entities.miscellaneous

import fr.shikkanime.entities.Genre
import fr.shikkanime.entities.Simulcast

data class GenreCoverage(
    val simulcast: Simulcast,
    val genre: Genre,
    val value: Double,
)