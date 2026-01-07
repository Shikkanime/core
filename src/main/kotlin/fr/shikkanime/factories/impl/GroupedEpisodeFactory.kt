package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.mappings.GroupedEpisodeDto
import fr.shikkanime.entities.EpisodeVariant
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

    /**
     * Converts a list of [EpisodeVariant] belonging to the same group into a single [GroupedEpisode].
     *
     * @param variants The list of variants in the group.
     * @return A [GroupedEpisode] object.
     */
    fun toEntity(variants: List<EpisodeVariant>): GroupedEpisode {
        val firstVariant = variants.first()
        val firstMapping = firstVariant.mapping!!

        val mappingUuids = variants.asSequence()
            .map { it.mapping!! }
            .sortedWith(compareBy({ it.season }, { it.episodeType!! }, { it.number }))
            .map { it.uuid!! }
            .toSet()

        val isSingleMapping = mappingUuids.size == 1

        return GroupedEpisode(
            anime = firstMapping.anime!!,
            releaseDateTime = variants.minOf { it.releaseDateTime },
            lastUpdateDateTime = variants.maxOf { it.mapping!!.lastUpdateDateTime },
            minSeason = variants.minOf { it.mapping!!.season!! },
            maxSeason = variants.maxOf { it.mapping!!.season!! },
            episodeType = firstMapping.episodeType!!,
            minNumber = variants.minOf { it.mapping!!.number!! },
            maxNumber = variants.maxOf { it.mapping!!.number!! },
            mappings = mappingUuids,
            variants = variants,
            title = if (isSingleMapping) firstMapping.title else null,
            description = if (isSingleMapping) firstMapping.description else null,
            duration = if (isSingleMapping) firstMapping.duration else null
        )
    }
}