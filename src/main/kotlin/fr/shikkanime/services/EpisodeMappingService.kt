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
import fr.shikkanime.utils.TelemetryConfig
import fr.shikkanime.utils.TelemetryConfig.trace
import java.time.ZonedDateTime
import java.util.*

class EpisodeMappingService : AbstractService<EpisodeMapping, EpisodeMappingRepository>() {
    private val tracer = TelemetryConfig.getTracer("EpisodeMappingService")
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
    ) = tracer.trace { episodeMappingRepository.findAllBy(countryCode, anime, season, sort, page, limit) }

    fun findAllByAnime(animeUuid: UUID) = tracer.trace { episodeMappingRepository.findAllByAnime(animeUuid) }

    fun findAllByAnime(anime: Anime) = findAllByAnime(anime.uuid!!)

    fun findAllNeedUpdate(lastUpdateDateTime: ZonedDateTime, lastImageUpdateDateTime: ZonedDateTime) =
        episodeMappingRepository.findAllNeedUpdate(lastUpdateDateTime, lastImageUpdateDateTime)

    fun findAllSeo() = episodeMappingRepository.findAllSeo()

    fun findAllSimulcasted(ignoreEpisodeTypes: Set<EpisodeType>, ignoreAudioLocale: String) =
        episodeMappingRepository.findAllSimulcasted(ignoreEpisodeTypes, ignoreAudioLocale)

    fun findAllGroupedBy(countryCode: CountryCode?, sort: List<SortParameter>, page: Int, limit: Int) = tracer.trace { episodeMappingRepository.findAllGroupedBy(countryCode, sort, page, limit) }

    fun findLastNumber(anime: Anime, episodeType: EpisodeType, season: Int, platform: Platform, audioLocale: String) =
        episodeMappingRepository.findLastNumber(anime, episodeType, season, platform, audioLocale)

    fun findByAnimeSeasonEpisodeTypeNumber(animeUuid: UUID, season: Int, episodeType: EpisodeType, number: Int) =
        episodeMappingRepository.findByAnimeSeasonEpisodeTypeNumber(animeUuid, season, episodeType, number)

    fun findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime: Anime, episode: EpisodeMapping) =
        episodeMappingRepository.findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime, episode)

    fun findMinimalReleaseDateTime() = tracer.trace { episodeMappingRepository.findMinimalReleaseDateTime() }

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