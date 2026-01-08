package fr.shikkanime.factories.impl

import fr.shikkanime.dtos.GenreDto
import fr.shikkanime.entities.Genre
import fr.shikkanime.factories.IGenericFactory

class GenreFactory : IGenericFactory<Genre, GenreDto> {
    override fun toDto(entity: Genre) = GenreDto(
        uuid = entity.uuid,
        name = entity.name!!,
    )
}
