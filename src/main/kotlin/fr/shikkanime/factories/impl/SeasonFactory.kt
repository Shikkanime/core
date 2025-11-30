package fr.shikkanime.factories.impl

import fr.shikkanime.dtos.SeasonDto
import fr.shikkanime.entities.miscellaneous.Season
import fr.shikkanime.factories.IGenericFactory
import fr.shikkanime.utils.withUTCString

class SeasonFactory : IGenericFactory<Season, SeasonDto> {
    override fun toDto(entity: Season): SeasonDto {
        return SeasonDto(
            entity.number,
            entity.releaseDateTime.withUTCString(),
            entity.lastReleaseDateTime.withUTCString(),
            entity.episodes
        )
    }
}