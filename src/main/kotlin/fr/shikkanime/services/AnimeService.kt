package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.weekly.WeeklyAnimeDto
import fr.shikkanime.dtos.weekly.WeeklyAnimesDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.*
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.factories.impl.AnimeFactory
import fr.shikkanime.factories.impl.EpisodeMappingFactory
import fr.shikkanime.factories.impl.PlatformFactory
import fr.shikkanime.repositories.AnimeRepository
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.utils.StringUtils.capitalizeWords
import fr.shikkanime.utils.indexers.GroupedIndexer
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AnimeService : AbstractService<Anime, AnimeRepository>() {
    @Inject private lateinit var animeRepository: AnimeRepository
    @Inject private lateinit var simulcastCacheService: SimulcastCacheService
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var simulcastService: SimulcastService
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var memberFollowAnimeService: MemberFollowAnimeService
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var platformFactory: PlatformFactory
    @Inject private lateinit var episodeMappingFactory: EpisodeMappingFactory

    override fun getRepository() = animeRepository

    fun findAllBy(
        countryCode: CountryCode?,
        simulcastUuid: UUID?,
        name: String?,
        searchTypes: Array<LangType>?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
    ) = animeRepository.findAllBy(countryCode, simulcastUuid, name, searchTypes, sort, page, limit)

    fun findAllBySimulcast(simulcastUuid: UUID) = animeRepository.findAllBySimulcast(simulcastUuid)

    fun findAllNeedUpdate(): List<Anime> {
        val simulcasts = simulcastCacheService.findAll()

        val currentSeasonDelay = configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_ANIME_DELAY_CURRENT_SEASON, 7).toLong()
        val lastSeasonDelay = configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_ANIME_DELAY_LAST_SEASON, 30).toLong()
        val othersDelay = configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_ANIME_DELAY_OTHERS, 90).toLong()

        return animeRepository.findAllNeedUpdate(
            currentSimulcastUuid = simulcasts.getOrNull(0)?.uuid,
            lastSimulcastUuid = simulcasts.getOrNull(1)?.uuid,
            currentSeasonDelay = currentSeasonDelay,
            lastSeasonDelay = lastSeasonDelay,
            othersDelay = othersDelay
        )
    }

    fun findAllAudioLocales(uuid: UUID) = animeRepository.findAllAudioLocales(uuid)

    fun findAllSeasons(uuid: UUID) = animeRepository.findAllSeasons(uuid)

    fun findAllSimulcastedWithAnimePlatformInvalid(simulcastUuids: Collection<UUID>, platform: Platform, lastValidateDateTime: ZonedDateTime, ignoreAudioLocale: String) = animeRepository.findAllSimulcastedWithAnimePlatformInvalid(simulcastUuids, platform, lastValidateDateTime, ignoreAudioLocale)

    fun findAllSlugs() = animeRepository.findAllSlugs()

    fun preIndex() = animeRepository.preIndex()

    fun findBySlug(countryCode: CountryCode, slug: String) = animeRepository.findBySlug(countryCode, slug)

    fun findByName(countryCode: CountryCode, name: String?) =
        animeRepository.findByName(countryCode, name)

    fun getWeeklyAnimes(
        countryCode: CountryCode,
        memberUuid: UUID?,
        startOfWeekDay: LocalDate,
        searchTypes: Array<LangType>? = null,
    ): List<WeeklyAnimesDto> {
        val zoneId = ZoneId.of(countryCode.timezone)
        val dayCountryPattern = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag(countryCode.locale))

        val startAtPreviousWeek = startOfWeekDay.minusWeeks(1).atStartOfDay(zoneId)
        val startOfCurrentWeek = startOfWeekDay.atStartOfDay(zoneId)
        val endOfCurrentWeek = startOfWeekDay.atEndOfWeek().atEndOfTheDay(zoneId)
        val currentWeekRange = startOfCurrentWeek..endOfCurrentWeek

        val followed = memberUuid?.let(memberFollowAnimeService::findAllFollowedAnimesUUID)

        val indexes = GroupedIndexer.filterReleasesByDateRange(
            countryCode,
            startAtPreviousWeek,
            endOfCurrentWeek,
        ).filter { (compositeIndex, datas) ->
            (followed?.contains(compositeIndex.animeUuid) ?: true) &&
                    (searchTypes?.let { datas.any { data -> LangType.fromAudioLocale(countryCode, data.audioLocale) in it } } ?: true)
        }

        val variants = episodeVariantService.findAllByUuids(indexes.flatMap { it.value.map { it.variantUuid } }.toSet())
            .associateBy { it.uuid }

        val groupedAnimes = indexes.mapNotNull { (compositeIndex, variantUuids) ->
            val zonedDateTime = compositeIndex.releaseDateTime
            val variants = variantUuids.mapNotNull { variants[it.variantUuid] }

            val isReleaseInCurrentWeek = zonedDateTime.withZoneSameInstant(zoneId) in currentWeekRange
            val anime = variants.first().mapping!!.anime!!
            val mappings = variants.takeIf { it.isNotEmpty() && isReleaseInCurrentWeek }
                ?.map { it.mapping!! }
                ?.distinctBy { it.uuid }
                ?.sortedWith(compareBy({ it.releaseDateTime }, { it.season }, { it.episodeType }, { it.number })) ?: emptyList()
            val mappingCount = mappings.takeIfNotEmpty()?.size ?: variants.map { it.mapping!!.uuid }.distinct().count()

            val matchingEpisodes = indexes.filter { (otherIndex, _) ->
                otherIndex.animeUuid == compositeIndex.animeUuid &&
                        otherIndex.episodeType == compositeIndex.episodeType
            }

            if (!isReleaseInCurrentWeek && (matchingEpisodes.any { it.key.releaseDateTime in currentWeekRange } || mappingCount > 5 || compositeIndex.episodeType == EpisodeType.FILM)) {
                return@mapNotNull null
            }

            WeeklyAnimeDto(
                animeFactory.toDto(anime),
                variants.map { platformFactory.toDto(it.platform!!) }.toTreeSet(),
                zonedDateTime.withUTCString(),
                buildString {
                    append("/animes/${anime.slug}")

                    val season = mappings.map { it.season }.distinct().singleOrNull()
                        ?: matchingEpisodes.flatMap { it.value.map { it.season } }.distinct().singleOrNull()

                    season?.let {
                        append("/season-$it")
                        if (mappings.size == 1) {
                            val episode = mappings.first()
                            append("/${episode.episodeType!!.slug}-${episode.number}")
                        }
                    }
                },
                variants.map { LangType.fromAudioLocale(countryCode, it.audioLocale!!) }.toTreeSet(),
                compositeIndex.episodeType,
                mappings.minOfOrNull { it.number!! },
                mappings.maxOfOrNull { it.number!! },
                mappings.firstOrNull()?.number,
                mappings.map { episodeMappingFactory.toDto(it, false) }.toSet()
            )
        }

        return (0..6).map { dayOffset ->
            val date = startOfWeekDay.plusDays(dayOffset.toLong())
            val tuplesDay = groupedAnimes.filter {
                ZonedDateTime.parse(it.releaseDateTime).withZoneSameInstant(zoneId).dayOfWeek.value == dayOffset + 1
            }

            WeeklyAnimesDto(
                date.format(dayCountryPattern).capitalizeWords(),
                tuplesDay.sortedWith(
                    compareBy(
                        { ZonedDateTime.parse(it.releaseDateTime).withZoneSameInstant(zoneId).toLocalTime() },
                        { it.anime.slug },
                        { it.episodeType },
                        { it.number }
                    )
                )
            )
        }
    }

    fun addSimulcastToAnime(anime: Anime, simulcast: Simulcast): Boolean {
        if (anime.simulcasts.none { it.uuid == simulcast.uuid }) {
            simulcast.uuid ?: simulcastService.save(simulcast)
            anime.simulcasts.add(simulcast)
            return true
        }

        return false
    }

    fun recalculateSimulcasts() {
        val ignoreEpisodeTypes = setOf(EpisodeType.SUMMARY)

        episodeMappingService.updateAllReleaseDate()
        animeRepository.updateAllReleaseDate()

        val simulcastRange = configCacheService.getValueAsInt(ConfigPropertyKey.SIMULCAST_RANGE, 1)
        val simulcastRangeDelay = configCacheService.getValueAsInt(ConfigPropertyKey.SIMULCAST_RANGE_DELAY, 3)
        val simulcasts = simulcastService.findAll().toMutableList()

        val groupedAnimes = findAll()
            .onEach { it.simulcasts = mutableSetOf() }
            .groupBy { it.countryCode!! }

        groupedAnimes.forEach { (countryCode, animes) ->
            val groupedMappings = episodeMappingService.findAllSimulcasted(ignoreEpisodeTypes, countryCode.locale)
                .groupBy { it.anime!!.uuid!! }

            animes.forEach { anime ->
                val episodeMappings = groupedMappings[anime.uuid] ?: return@forEach

                episodeMappings.forEach { episodeMapping ->
                    val previousReleaseDateTime = episodeMappings.filter {
                        it.releaseDateTime < episodeMapping.releaseDateTime &&
                                it.episodeType == episodeMapping.episodeType
                    }.maxOfOrNull { it.releaseDateTime }

                    val simulcast = episodeVariantService.getSimulcast(
                        simulcastRange,
                        simulcastRangeDelay,
                        anime,
                        episodeMapping,
                        previousReleaseDateTime,
                        sqlCheck = false,
                        simulcasts = simulcasts
                    )
                    addSimulcastToAnime(anime, simulcast)

                    if (simulcasts.none { it.uuid == simulcast.uuid }) {
                        simulcasts.add(simulcast)
                    }
                }
            }
        }

        updateAll(groupedAnimes.values.flatten())
    }

    override fun save(entity: Anime): Anime {
        entity.simulcasts = entity.simulcasts.map { simulcast ->
            simulcastService.findBySeasonAndYear(simulcast.season!!, simulcast.year!!) ?: simulcastService.save(simulcast)
        }.toMutableSet()

        entity.description = entity.description?.replace("\n", StringUtils.EMPTY_STRING)?.replace("\r", "")
        val savedEntity = super.save(entity)
        traceActionService.createTraceAction(savedEntity, TraceAction.Action.CREATE)
        return savedEntity
    }

    override fun delete(entity: Anime) {
        episodeMappingService.findAllByAnime(entity).forEach { episodeMappingService.delete(it) }
        memberFollowAnimeService.findAllByAnime(entity).forEach { memberFollowAnimeService.delete(it) }
        animePlatformService.findAllByAnime(entity).forEach { animePlatformService.delete(it) }
        super.delete(entity)
        traceActionService.createTraceAction(entity, TraceAction.Action.DELETE)
    }
}