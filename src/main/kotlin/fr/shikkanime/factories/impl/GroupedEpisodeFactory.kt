package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.mappings.GroupedEpisodeDto
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.GroupedEpisode
import fr.shikkanime.factories.IGenericFactory
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.takeIfNotEmpty
import fr.shikkanime.utils.toTreeSet
import fr.shikkanime.utils.withUTCString

class GroupedEpisodeFactory : IGenericFactory<GroupedEpisode, GroupedEpisodeDto> {
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var episodeSourceFactory: EpisodeSourceFactory

    private data class MappingKey(
        val season: Int,
        val episodeType: EpisodeType,
        val number: Int
    )

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
     * Converts a list of [EpisodeVariant] into multiple [GroupedEpisode] objects.
     *
     * Grouping rules:
     * - Episodes are grouped by anime and episode type
     * - Consecutive episodes (by season and number) are merged into a single group
     * - Different languages (VF/VOSTFR) are merged if they cover exactly the same episodes
     * - If languages cover different episodes, they are separated into different groups
     *
     * @param variants The list of variants to group.
     * @return A list of [GroupedEpisode] objects.
     */
    fun toEntities(variants: List<EpisodeVariant>): List<GroupedEpisode> {
        if (variants.isEmpty()) return emptyList()

        return variants
            .groupBy { it.mapping?.anime?.uuid }
            .values
            .flatMap { animeVariants -> groupByAnime(animeVariants) }
    }

    private fun groupByAnime(animeVariants: List<EpisodeVariant>): List<GroupedEpisode> {
        // Group variants by language type
        val variantsByLang = animeVariants.groupBy { variant ->
            val countryCode = variant.mapping!!.anime!!.countryCode!!
            LangType.fromAudioLocale(countryCode, variant.audioLocale!!)
        }

        // Get mappings (season, episodeType, number) by language
        val mappingsByLang = variantsByLang.mapValues { (_, langVariants) ->
            langVariants.map { variant ->
                MappingKey(
                    variant.mapping!!.season!!,
                    variant.mapping!!.episodeType!!,
                    variant.mapping!!.number!!
                )
            }.toSet()
        }

        // Find common mappings across all languages
        val commonMappings = if (mappingsByLang.isNotEmpty()) {
            mappingsByLang.values.reduce { acc, set -> acc.intersect(set) }
        } else {
            emptySet()
        }

        // Separate variants into common (same episodes across languages) and non-common
        val commonVariants = animeVariants.filter { variant ->
            MappingKey(
                variant.mapping!!.season!!,
                variant.mapping!!.episodeType!!,
                variant.mapping!!.number!!
            ) in commonMappings
        }

        val nonCommonVariants = animeVariants.filter { variant ->
            MappingKey(
                variant.mapping!!.season!!,
                variant.mapping!!.episodeType!!,
                variant.mapping!!.number!!
            ) !in commonMappings
        }

        val groups = mutableListOf<GroupedEpisode>()

        // Process common variants as a single group (merge languages)
        if (commonVariants.isNotEmpty()) {
            groups.addAll(groupConsecutiveEpisodes(commonVariants))
        }

        // Process non-common variants by language
        nonCommonVariants.groupBy { variant ->
            val countryCode = variant.mapping!!.anime!!.countryCode!!
            LangType.fromAudioLocale(countryCode, variant.audioLocale!!)
        }.values.forEach { langVariants ->
            groups.addAll(groupConsecutiveEpisodes(langVariants))
        }

        return groups
    }

    /**
     * Groups consecutive episodes of the same episode type.
     * Episodes are consecutive if:
     * - They have the same season and consecutive numbers (1, 2, 3...)
     * - Or they start a new season with number 1 after the previous season
     */
    private fun groupConsecutiveEpisodes(variants: List<EpisodeVariant>): List<GroupedEpisode> {
        if (variants.isEmpty()) return emptyList()

        return variants
            .groupBy { it.mapping!!.episodeType!! }
            .values
            .flatMap { groupConsecutiveByMapping(it).map(::toEntity) }
    }

    private fun groupConsecutiveByMapping(variants: List<EpisodeVariant>): List<List<EpisodeVariant>> {
        val sortedMappingGroups = variants
            .groupBy { it.mapping!!.uuid!! }
            .values
            .sortedWith(compareBy(
                { it.first().mapping!!.season!! },
                { it.first().mapping!!.number!! }
            ))

        if (sortedMappingGroups.isEmpty()) return emptyList()

        return buildList {
            var currentGroup = sortedMappingGroups.first().toMutableList()

            for (mappingVariants in sortedMappingGroups.drop(1)) {
                val lastMapping = currentGroup.last().mapping!!
                val currentMapping = mappingVariants.first().mapping!!

                if (isConsecutive(lastMapping, currentMapping)) {
                    currentGroup.addAll(mappingVariants)
                } else {
                    add(currentGroup)
                    currentGroup = mappingVariants.toMutableList()
                }
            }

            add(currentGroup)
        }
    }

    private fun isConsecutive(previous: EpisodeMapping, next: EpisodeMapping): Boolean {
        val (prevSeason, prevNumber) = previous.season!! to previous.number!!
        val (nextSeason, nextNumber) = next.season!! to next.number!!

        return (prevSeason == nextSeason && nextNumber == prevNumber + 1) ||
                (nextSeason == prevSeason + 1 && nextNumber == 1)
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