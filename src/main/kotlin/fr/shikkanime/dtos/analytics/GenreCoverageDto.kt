package fr.shikkanime.dtos.analytics

import fr.shikkanime.dtos.GenreDto
import fr.shikkanime.dtos.SimulcastDto
import java.io.Serializable

data class GenreCoverageDto(
    val simulcast: SimulcastDto,
    val genre: GenreDto,
    val value: Double,
) : Serializable