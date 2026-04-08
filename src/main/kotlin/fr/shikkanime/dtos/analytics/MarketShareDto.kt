package fr.shikkanime.dtos.analytics

import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.SimulcastDto
import java.io.Serializable

data class MarketShareDto(
    val simulcast: SimulcastDto,
    val platform: PlatformDto,
    val value: Double,
) : Serializable