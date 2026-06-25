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

    suspend fun findAllBy(
        countryCode: CountryCode?,
        animeUuid: UUID?,
        season: Int?,
        searchTypes: Array<LangType>?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
    ) = repository.findAllBy(countryCode, animeUuid, season, searchTypes, sort, page, limit)

    suspend fun findAllByAnime(animeUuid: UUID) = repository.findAllByAnime(animeUuid)

    suspend fun findAllByAnime(anime: Anime) = findAllByAnime(anime.uuid!!)

    suspend fun findAllNeedUpdate(): List<EpisodeMapping> {
        val simulcasts = simulcastCacheService.findAll()

        val currentSeasonDelay = configCacheService.getValueAsLong(ConfigPropertyKey.UPDATE_EPISODE_DELAY_CURRENT_SEASON, 7)
        val lastSeasonDelay = configCacheService.getValueAsLong(ConfigPropertyKey.UPDATE_EPISODE_DELAY_LAST_SEASON, 30)
        val othersDelay = configCacheService.getValueAsLong(ConfigPropertyKey.UPDATE_EPISODE_DELAY_OTHERS, 90)
        val lastImageUpdateDelay = configCacheService.getValueAsLong(ConfigPropertyKey.UPDATE_IMAGE_EPISODE_DELAY, 2)

        return repository.findAllNeedUpdate(
            currentSimulcastUuid = simulcasts.getOrNull(0)?.uuid,
            lastSimulcastUuid = simulcasts.getOrNull(1)?.uuid,
            currentSeasonDelay = currentSeasonDelay,
            lastSeasonDelay = lastSeasonDelay,
            othersDelay = othersDelay,
            lastImageUpdateDelay = lastImageUpdateDelay
        )
    }

    suspend fun findAllSeo() = repository.findAllSeo()

    suspend fun findAllSimulcasted(ignoreAudioLocale: String, ignoreEpisodeTypes: Set<EpisodeType>) =
        repository.findAllSimulcasted(ignoreAudioLocale, ignoreEpisodeTypes)

    suspend fun findLastNumber(anime: Anime, episodeType: EpisodeType, season: Int, platform: Platform, audioLocale: String) =
        repository.findLastNumber(anime, episodeType, season, platform, audioLocale)

    suspend fun findByAnimeSeasonEpisodeTypeNumber(animeUuid: UUID, season: Int, episodeType: EpisodeType, number: Int) =
        repository.findByAnimeSeasonEpisodeTypeNumber(animeUuid, season, episodeType, number)

    suspend fun findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime: Anime, episode: EpisodeCalculateDto) =
        repository.findPreviousReleaseDateOfSimulcastedEpisodeMapping(anime, episode)

    suspend fun findMinimalReleaseDateTime() = repository.findMinimalReleaseDateTime()

    suspend fun updateAllReleaseDate() = repository.updateAllReleaseDate()

    suspend fun updateAllSimulcast(map: Map<UUID, UUID>) = repository.updateAllSimulcast(map)

    override suspend fun delete(entity: EpisodeMapping) {
        episodeVariantService.findAllByMapping(entity).forEach { episodeVariantService.delete(it) }
        memberFollowEpisodeService.findAllByEpisode(entity).forEach { memberFollowEpisodeService.delete(it) }
        super.delete(entity)
    }
}