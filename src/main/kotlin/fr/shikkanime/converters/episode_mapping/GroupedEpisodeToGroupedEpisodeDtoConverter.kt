package fr.shikkanime.converters.episode_mapping

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.mappings.GroupedEpisodeDto
import fr.shikkanime.entities.GroupedEpisode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.withUTCString

class GroupedEpisodeToGroupedEpisodeDtoConverter : AbstractConverter<GroupedEpisode, GroupedEpisodeDto>() {
    @Converter
    fun convert(from: GroupedEpisode): GroupedEpisodeDto {
        val season = if (from.minSeason == from.maxSeason) from.minSeason.toString() else "${from.minSeason} - ${from.maxSeason}"
        val number = if (from.minNumber == from.maxNumber) from.minNumber.toString() else "${from.minNumber} - ${from.maxNumber}"
        val internalUrl = if (from.mappings.isNotEmpty()) {
            buildString {
                append(Constant.baseUrl)
                append("/animes/")
                append(from.anime.slug!!)
                if (from.minSeason == from.maxSeason) {
                    append("/season-${from.minSeason}")
                    if (from.minNumber == from.maxNumber) {
                        append("/${from.episodeType.slug}-${from.minNumber}")
                    }
                }
            }
        } else null

        return GroupedEpisodeDto(
            anime = convert(from.anime, AnimeDto::class.java),
            platforms = convert(from.platforms.sorted().toSet(), PlatformDto::class.java)!!,
            releaseDateTime = from.releaseDateTime.withUTCString(),
            lastUpdateDateTime = from.lastUpdateDateTime.withUTCString(),
            season = season,
            episodeType = from.episodeType,
            number = number,
            langTypes = from.audioLocales.map { LangType.fromAudioLocale(from.anime.countryCode!!, it) }.sorted().toSet(),
            title = from.title,
            description = from.description,
            duration = from.duration,
            internalUrl = internalUrl,
            mappings = from.mappings,
            urls = from.urls
        )
    }
}