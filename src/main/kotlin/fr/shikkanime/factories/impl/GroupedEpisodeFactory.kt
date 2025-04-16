package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.mappings.GroupedEpisodeDto
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.views.GroupedEpisodeView
import fr.shikkanime.factories.IGenericFactory
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.withUTCString

class GroupedEpisodeFactory : IGenericFactory<GroupedEpisodeView, GroupedEpisodeDto> {
    @Inject
    private lateinit var animeFactory: AnimeFactory

    @Inject
    private lateinit var platformFactory: PlatformFactory

    override fun toDto(entity: GroupedEpisodeView): GroupedEpisodeDto {
        val season = if (entity.minSeason == entity.maxSeason) entity.minSeason.toString() else "${entity.minSeason} - ${entity.maxSeason}"
        val number = if (entity.minNumber == entity.maxNumber) entity.minNumber.toString() else "${entity.minNumber} - ${entity.maxNumber}"
        val internalUrl = if (entity.episodeMappingUuids!!.isNotEmpty()) {
            buildString {
                append(Constant.baseUrl)
                append("/animes/")
                append(entity.anime!!.slug!!)
                if (entity.minSeason == entity.maxSeason) {
                    append("/season-${entity.minSeason}")
                    if (entity.minNumber == entity.maxNumber) {
                        append("/${entity.episodeType!!.slug}-${entity.minNumber}")
                    }
                }
            }
        } else null

        return GroupedEpisodeDto(
            anime = animeFactory.toDto(entity.anime!!),
            platforms = entity.platforms!!.sortedBy { it.name }.map { platformFactory.toDto(it) }.toSet(),
            releaseDateTime = entity.minReleaseDateTime!!.withUTCString(),
            lastUpdateDateTime = entity.maxLastUpdateDateTime!!.withUTCString(),
            season = season,
            episodeType = entity.episodeType!!,
            number = number,
            langTypes = entity.audioLocales!!.map { LangType.fromAudioLocale(entity.anime.countryCode!!, it) }.sorted().toSet(),
            title = entity.title,
            description = entity.description,
            duration = entity.duration,
            internalUrl = internalUrl,
            mappings = entity.episodeMappingUuids,
            urls = entity.urls!!.toSet()
        )
    }
}