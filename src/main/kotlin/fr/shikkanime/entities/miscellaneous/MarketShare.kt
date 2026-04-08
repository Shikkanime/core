package fr.shikkanime.entities.miscellaneous

import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.Platform

data class MarketShare(
    val simulcast: Simulcast,
    val platform: Platform,
    val value: Double,
)