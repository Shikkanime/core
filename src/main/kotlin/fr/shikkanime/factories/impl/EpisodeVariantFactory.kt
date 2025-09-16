package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.factories.IEpisodeVariantFactory
import fr.shikkanime.utils.withUTCString

class EpisodeVariantFactory : IEpisodeVariantFactory {
    @Inject private lateinit var episodeMappingFactory: EpisodeMappingFactory
    @Inject private lateinit var platformFactory: PlatformFactory

    override fun toDto(
        entity: EpisodeVariant,
        useMapping: Boolean
    ) = EpisodeVariantDto(
        uuid = entity.uuid!!,
        mapping = episodeMappingFactory.takeIf { useMapping }?.toDto(entity.mapping!!),
        releaseDateTime = entity.releaseDateTime.withUTCString(),
        platform = platformFactory.toDto(entity.platform!!),
        audioLocale = entity.audioLocale!!,
        identifier = entity.identifier!!,
        url = entity.url!!,
        uncensored = entity.uncensored
    )
}