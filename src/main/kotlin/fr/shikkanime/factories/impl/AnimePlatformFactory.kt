package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.AnimePlatformDto
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.factories.IGenericFactory

class AnimePlatformFactory : IGenericFactory<AnimePlatform, AnimePlatformDto> {
    @Inject
    private lateinit var platformFactory: PlatformFactory

    override fun toDto(entity: AnimePlatform) = AnimePlatformDto(
        uuid = entity.uuid,
        platform = platformFactory.toDto(entity.platform!!),
        platformId = entity.platformId!!,
    )
}