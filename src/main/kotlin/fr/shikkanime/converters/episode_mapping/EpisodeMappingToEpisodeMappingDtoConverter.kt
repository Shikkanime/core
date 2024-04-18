package fr.shikkanime.converters.episode_mapping

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.EpisodeMappingDto
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.variants.EpisodeVariantWithoutMappingDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.utils.withUTCString

class EpisodeMappingToEpisodeMappingDtoConverter : AbstractConverter<EpisodeMapping, EpisodeMappingDto>() {
    override fun convert(from: EpisodeMapping): EpisodeMappingDto {
        val variants = from.variants.sortedBy { it.releaseDateTime }

        return EpisodeMappingDto(
            uuid = from.uuid!!,
            anime = convert(from.anime, AnimeDto::class.java),
            releaseDateTime = from.releaseDateTime.withUTCString(),
            lastReleaseDateTime = from.lastReleaseDateTime.withUTCString(),
            lastUpdateDateTime = from.lastUpdateDateTime.withUTCString(),
            episodeType = from.episodeType!!,
            season = from.season!!,
            number = from.number!!,
            duration = from.duration,
            title = from.title,
            description = from.description,
            image = from.image!!,
            variants = convert(variants, EpisodeVariantWithoutMappingDto::class.java),
            platforms = convert(
                variants.mapNotNull { it.platform }.sortedBy { it.name }.toSet(),
                PlatformDto::class.java
            ),
            langTypes = variants.map { LangType.fromAudioLocale(from.anime!!.countryCode!!, it.audioLocale!!) }
                .sorted()
                .toSet(),
            status = from.status
        )
    }
}