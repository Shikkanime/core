package fr.shikkanime.factories

import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.entities.EpisodeMapping

interface IEpisodeMappingFactory : IGenericFactory<EpisodeMapping, EpisodeMappingDto> {
    fun toDto(entity: EpisodeMapping, useAnime: Boolean = true): EpisodeMappingDto

    override fun toDto(entity: EpisodeMapping) = toDto(entity, true)
}