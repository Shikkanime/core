package fr.shikkanime.factories.impl

import fr.shikkanime.dtos.TagDto
import fr.shikkanime.entities.Tag
import fr.shikkanime.factories.IGenericFactory

class TagFactory : IGenericFactory<Tag, TagDto> {
    override fun toDto(entity: Tag) = TagDto(
        uuid = entity.uuid,
        name = entity.name!!,
    )
}
