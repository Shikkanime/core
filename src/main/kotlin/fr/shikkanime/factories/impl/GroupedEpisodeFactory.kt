package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.mappings.GroupedEpisodeDto
import fr.shikkanime.entities.miscellaneous.GroupedEpisode
import fr.shikkanime.factories.IGenericFactory
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.takeIfNotEmpty
import fr.shikkanime.utils.toTreeSet
import fr.shikkanime.utils.withUTCString

class GroupedEpisodeFactory : IGenericFactory<GroupedEpisode, GroupedEpisodeDto> {
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var episodeSourceFactory: EpisodeSourceFactory

    override fun toDto(entity: GroupedEpisode): GroupedEpisodeDto {
        val season = if (entity.minSeason == entity.maxSeason) entity.minSeason.toString() else "${entity.minSeason} - ${entity.maxSeason}"
        val number = if (entity.minNumber == entity.maxNumber) entity.minNumber.toString() else "${entity.minNumber} - ${entity.maxNumber}"
        val internalUrl = entity.mappings.takeIfNotEmpty()?.let {
            buildString {
                append(Constant.baseUrl)
                append("/animes/${entity.anime.slug!!}")
                if (entity.minSeason == entity.maxSeason) {
                    append("/season-${entity.minSeason}")
                    if (entity.minNumber == entity.maxNumber) {
                        append("/${entity.episodeType.slug}-${entity.minNumber}")
                    }
                }
            }
        }

        return GroupedEpisodeDto(
            anime = animeFactory.toDto(entity.anime),
            releaseDateTime = entity.releaseDateTime.withUTCString(),
            lastUpdateDateTime = entity.lastUpdateDateTime.withUTCString(),
            season = season,
            episodeType = entity.episodeType,
            number = number,
            title = entity.title,
            description = entity.description,
            duration = entity.duration,
            internalUrl = internalUrl,
            mappings = entity.mappings.toSet(),
            sources = entity.variants.map(episodeSourceFactory::toDto).toTreeSet()
        )
    }
}