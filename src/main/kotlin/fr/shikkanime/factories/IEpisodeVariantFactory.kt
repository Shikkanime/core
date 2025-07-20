package fr.shikkanime.factories

import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.EpisodeVariant

interface IEpisodeVariantFactory : IGenericFactory<EpisodeVariant, EpisodeVariantDto> {
    fun toDto(entity: EpisodeVariant, useMapping: Boolean = true): EpisodeVariantDto

    override fun toDto(entity: EpisodeVariant) = toDto(entity, true)
}