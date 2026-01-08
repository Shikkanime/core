package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.animes.AnimeTagDto
import fr.shikkanime.entities.AnimeTag
import fr.shikkanime.factories.IGenericFactory

class AnimeTagFactory : IGenericFactory<AnimeTag, AnimeTagDto> {
    @Inject
    private lateinit var tagFactory: TagFactory

    override fun toDto(entity: AnimeTag) = AnimeTagDto(
        uuid = entity.uuid,
        tag = tagFactory.toDto(entity.tag!!),
        isAdult = entity.isAdult,
        isSpoiler = entity.isSpoiler,
    )
}
