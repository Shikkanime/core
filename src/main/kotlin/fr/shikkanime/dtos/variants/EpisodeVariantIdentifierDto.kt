package fr.shikkanime.dtos.variants

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import java.util.UUID

data class EpisodeVariantIdentifierDto(
    val countryCode: CountryCode,
    val animeUuid: UUID,
    val platform: Platform,
    val season: Int,
    val episodeType: EpisodeType,
    val number: Int,
    val audioLocale: String,
    val identifier: String,
)
