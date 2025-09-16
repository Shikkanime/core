package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.EpisodeSourceDto
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.factories.IGenericFactory

class EpisodeSourceFactory : IGenericFactory<EpisodeVariant, EpisodeSourceDto> {
    @Inject private lateinit var platformFactory: PlatformFactory

    override fun toDto(entity: EpisodeVariant) = EpisodeSourceDto(
        platform = platformFactory.toDto(entity.platform!!),
        url = entity.url!!,
        langType = LangType.fromAudioLocale(entity.mapping!!.anime!!.countryCode!!, entity.audioLocale!!)
    )

    fun toDto(countryCode: CountryCode, dto: EpisodeVariantDto) = EpisodeSourceDto(
        platform = dto.platform,
        url = dto.url,
        langType = LangType.fromAudioLocale(countryCode, dto.audioLocale)
    )
}