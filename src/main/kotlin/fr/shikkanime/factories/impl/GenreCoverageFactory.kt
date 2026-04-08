package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.analytics.GenreCoverageDto
import fr.shikkanime.entities.miscellaneous.GenreCoverage
import fr.shikkanime.factories.IGenericFactory

class GenreCoverageFactory : IGenericFactory<GenreCoverage, GenreCoverageDto> {
    @Inject
    private lateinit var simulcastFactory: SimulcastFactory
    @Inject
    private lateinit var genreFactory: GenreFactory

    override fun toDto(entity: GenreCoverage) = GenreCoverageDto(
        simulcast = simulcastFactory.toDto(entity.simulcast),
        genre = genreFactory.toDto(entity.genre),
        value = entity.value
    )
}