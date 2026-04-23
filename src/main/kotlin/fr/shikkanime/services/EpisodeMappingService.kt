package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.EpisodeCalculateDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.*
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.repositories.EpisodeMappingRepository
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import java.util.*

class EpisodeMappingService : AbstractService<EpisodeMapping, EpisodeMappingRepository>() {
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService
    @Inject private lateinit var simulcastCacheService: SimulcastCacheService
    @Inject private lateinit var configCacheService: ConfigCacheService

    fun findAllBy(
        countryCode: CountryCode?,
        animeUuid: UUID?,
        season: Int?,
        searchTypes: Array<LangType>?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
    ) = repository.findAllBy(countryCode, animeUuid, season, searchTypes, sort, page, limit)

    fun findAllByAnime(anime: Anime) = repository.findAllByAnime(anime.uuid!!)

    fun findAllByAnime(animeUuid: UUID) = repository.findAllByAnime(animeUuid)

    fun findAllNeedUpdate(): List<EpisodeMapping> {
        val simulcasts = simulcastCacheService.findAll()

        val currentSeasonDelay = configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_EPISODE_DELAY_CURRENT_SEASON, 7).toLong()
        val lastSeasonDelay = configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_EPISODE_DELAY_LAST_SEASON, 30).toLong()
        val othersDelay = configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_EPISODE_DELAY_OTHERS, 90).toLong()
        val lastImageUpdateDelay = configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_IMAGE_EPISODE_DELAY, 2).toLong()

        return repository.findAllNeedUpdate(
            currentSimulcastUuid = simulcasts.getOrNull(0)?.uuid,
            lastSimulcastUuid = simulcasts.getOrNull(1)?.uuid,
            currentSeasonDelay = currentSeasonDelay,
            lastSeasonDelay = lastSeasonDelay,
            othersDelay = othersDelay,
            lastImageUpdateDelay = lastImageUpdateDelay
        )
    }

    fun findAllSeo() = repository.findAllSeo()

    fun findAllSimulcasted(ignoreAudioLocale: String, ignoreEpisodeTypes: Set<EpisodeType>) =
        repository.findAllSimulcasted(ignoreAudioLocale, ignoreEpisodeTypes)

    fun findLastNumber(anime: Anime, episodeType: EpisodeType, season: Int, platform: Platform, audioLocale: String) =
        repository.findLastNumber(anime, episodeType, season, platform, audioLocale)

    fun findByAnimeSeasonEpisodeTypeNumber(animeUuid: UUID, season: Int, episodeType: EpisodeType, number: Int) =
        repository.findByAnimeSeasonEpisodeTypeNumber(animeUuid, season, episodeType, number)

    fun findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime: Anime, episode: EpisodeCalculateDto) =
        repository.findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime, episode)

    fun findMinimalReleaseDateTime() = repository.findMinimalReleaseDateTime()

    fun updateAllReleaseDate() = repository.updateAllReleaseDate()

    fun updateAllSimulcast(map: Map<UUID, UUID>) = repository.updateAllSimulcast(map)

    override fun delete(entity: EpisodeMapping) {
        episodeVariantService.findAllByMapping(entity).forEach { episodeVariantService.delete(it) }
        memberFollowEpisodeService.findAllByEpisode(entity).forEach { memberFollowEpisodeService.delete(it) }
        super.delete(entity)
    }
}