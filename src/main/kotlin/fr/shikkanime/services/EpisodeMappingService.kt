package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.repositories.EpisodeMappingRepository
import java.time.ZonedDateTime
import java.util.*

class EpisodeMappingService : AbstractService<EpisodeMapping, EpisodeMappingRepository>() {
    @Inject private lateinit var episodeMappingRepository: EpisodeMappingRepository
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService
    @Inject private lateinit var traceActionService: TraceActionService

    override fun getRepository() = episodeMappingRepository

    fun findAllBy(
        countryCode: CountryCode?,
        anime: Anime?,
        season: Int?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
    ) = episodeMappingRepository.findAllBy(countryCode, anime, season, sort, page, limit)

    fun findAllByAnime(anime: Anime) = episodeMappingRepository.findAllByAnime(anime.uuid!!)

    fun findAllByAnime(animeUuid: UUID) = episodeMappingRepository.findAllByAnime(animeUuid)

    fun findAllNeedUpdateByPlatforms(platforms: List<Platform>, lastDateTime: ZonedDateTime) =
        episodeMappingRepository.findAllNeedUpdateByPlatforms(platforms, lastDateTime)

    fun findAllSeo() = episodeMappingRepository.findAllSeo()

    fun findAllSimulcasted(ignoreEpisodeTypes: Set<EpisodeType>, ignoreAudioLocale: String) =
        episodeMappingRepository.findAllSimulcasted(ignoreEpisodeTypes, ignoreAudioLocale)

    fun findAllGroupedBy(countryCode: CountryCode?, page: Int, limit: Int) = episodeMappingRepository.findAllGroupedBy(countryCode, page, limit)

    fun findLastNumber(anime: Anime, episodeType: EpisodeType, season: Int, platform: Platform, audioLocale: String) =
        episodeMappingRepository.findLastNumber(anime, episodeType, season, platform, audioLocale)

    fun findByAnimeSeasonEpisodeTypeNumber(animeUuid: UUID, season: Int, episodeType: EpisodeType, number: Int) =
        episodeMappingRepository.findByAnimeSeasonEpisodeTypeNumber(animeUuid, season, episodeType, number)

    fun findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime: Anime, episode: EpisodeMapping) =
        episodeMappingRepository.findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime, episode)

    fun findMinimalReleaseDateTime() = episodeMappingRepository.findMinimalReleaseDateTime()

    fun updateAllReleaseDate() = episodeMappingRepository.updateAllReleaseDate()

    override fun save(entity: EpisodeMapping): EpisodeMapping {
        val save = super.save(entity)
        traceActionService.createTraceAction(save, TraceAction.Action.CREATE)
        return save
    }

    override fun delete(entity: EpisodeMapping) {
        episodeVariantService.findAllByMapping(entity).forEach { episodeVariantService.delete(it) }
        memberFollowEpisodeService.findAllByEpisode(entity).forEach { memberFollowEpisodeService.delete(it) }
        super.delete(entity)
        traceActionService.createTraceAction(entity, TraceAction.Action.DELETE)
    }
}