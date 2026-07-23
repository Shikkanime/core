package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.weekly.WeeklyAnimeDto
import fr.shikkanime.dtos.weekly.WeeklyAnimesDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.Simulcast
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
import fr.shikkanime.utils.indexers.NewGroupIndexer
import fr.shikkanime.utils.indexers.NewGroupIndexer.ElementVariant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Locale

class AnimeService : AbstractService<Anime, AnimeRepository>() {
    @Inject private lateinit var simulcastCacheService: SimulcastCacheService
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var simulcastService: SimulcastService
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var memberFollowAnimeService: MemberFollowAnimeService
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject private lateinit var animeTagService: AnimeTagService
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var platformFactory: PlatformFactory
    @Inject private lateinit var episodeMappingFactory: EpisodeMappingFactory

    suspend fun findAllBy(
        countryCode: CountryCode?,
        simulcastUuid: UUID?,
        name: String?,
        searchTypes: Array<LangType>?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
    ) = repository.findAllBy(countryCode, simulcastUuid, name, searchTypes, sort, page, limit)

    suspend fun findAllBySimulcast(simulcastUuid: UUID) = repository.findAllBySimulcast(simulcastUuid)

    suspend fun findAllNeedUpdate(): List<Anime> {
        val simulcasts = simulcastCacheService.findAll()

        val currentSeasonDelay = configCacheService.getValueAsLong(ConfigPropertyKey.UPDATE_ANIME_DELAY_CURRENT_SEASON, 7)
        val lastSeasonDelay = configCacheService.getValueAsLong(ConfigPropertyKey.UPDATE_ANIME_DELAY_LAST_SEASON, 30)
        val othersDelay = configCacheService.getValueAsLong(ConfigPropertyKey.UPDATE_ANIME_DELAY_OTHERS, 90)

        return repository.findAllNeedUpdate(
            currentSimulcastUuid = simulcasts.getOrNull(0)?.uuid,
            lastSimulcastUuid = simulcasts.getOrNull(1)?.uuid,
            currentSeasonDelay = currentSeasonDelay,
            lastSeasonDelay = lastSeasonDelay,
            othersDelay = othersDelay
        )
    }

    suspend fun findAllAudioLocales(uuid: UUID) = repository.findAllAudioLocales(uuid)

    suspend fun findAllSeasons(uuid: UUID) = repository.findAllSeasons(uuid)

    suspend fun findAllSimulcastedWithAnimePlatformInvalid(
        simulcastUuids: Collection<UUID>,
        platform: Platform,
        lastUpdateDateTime: ZonedDateTime,
        ignoreAudioLocale: String
    ) = repository.findAllSimulcastedWithAnimePlatformInvalid(
        simulcastUuids,
        platform,
        lastUpdateDateTime,
        ignoreAudioLocale
    )

    suspend fun findAllSlugs() = repository.findAllSlugs()

    suspend fun preIndex() = repository.preIndex()

    suspend fun findBySlug(countryCode: CountryCode, slug: String) = repository.findBySlug(countryCode, slug)

    suspend fun findByName(countryCode: CountryCode, name: String?) =
        repository.findByName(countryCode, name)

    suspend fun getWeeklyAnimes(
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

        val followed = memberUuid?.let { memberFollowAnimeService.findAllFollowedAnimesUUID(it) }

        val predicate: (ElementVariant) -> Boolean = { variant -> searchTypes?.contains(variant.langType) == true }

        val elements = NewGroupIndexer.getElements(
            predicate = { element ->
                element.countryCode == countryCode
                        && element.releaseDateTime in startAtPreviousWeek..endOfCurrentWeek
                        && (followed?.contains(element.animeUuid) ?: true)
                        && (searchTypes?.let { element.variants.any { variant -> variant.langType in it } } ?: true)
            },
            comparator = compareBy<NewGroupIndexer.Element> { it.releaseDateTime }
                .thenBy { it.animeSlug }
                .thenBy { it.episodeType }
        )

        val groupedElements = elements.groupBy { Triple(it.countryCode, it.animeUuid, it.episodeType) }

        val variantUuids = elements.flatMap { element ->
            element.variants
                .filterNotNull(predicate.takeIf { !searchTypes.isNullOrEmpty() })
                .map(ElementVariant::uuid)
        }.toLinkedSet()

        val variants = episodeVariantService.findAllByUuids(variantUuids)
            .associateBy { it.uuid }

        val releases = elements.mapNotNull { element ->
            val groupedElement = groupedElements[Triple(element.countryCode, element.animeUuid, element.episodeType)] ?: return@mapNotNull null
            val variants = element.variants.mapNotNull { variants[it.uuid] }.takeIfNotEmpty() ?: return@mapNotNull null

            val isReleaseInCurrentWeek = element.releaseDateTime.withZoneSameInstant(zoneId) in currentWeekRange
            val anime = variants.first().mapping?.anime ?: return@mapNotNull null
            val mappings = variants.takeIf { isReleaseInCurrentWeek }
                ?.mapNotNull { it.mapping }
                ?.distinctBy { it.uuid }
                ?.sortedWith(compareBy({ it.releaseDateTime }, { it.season }, { it.episodeType }, { it.number })) ?: emptyList()
            val mappingCount = mappings.takeIfNotEmpty()?.size ?: variants.mapNotNull { it.mapping?.uuid }.distinct().count()

            if (!isReleaseInCurrentWeek
                && (groupedElement.any { it.releaseDateTime.withZoneSameInstant(zoneId) in currentWeekRange }
                        || mappingCount > 5
                        || element.episodeType in listOf(EpisodeType.FILM, EpisodeType.SUMMARY)))
                return@mapNotNull null

            WeeklyAnimeDto(
                animeFactory.toDto(anime),
                variants.map { platformFactory.toDto(it.platform!!) }.toTreeSet(),
                element.releaseDateTime.withUTCString(),
                buildString {
                    append("/animes/${anime.slug}")

                    val season = mappings.mapNotNull(EpisodeMapping::season).distinct().singleOrNull()
                        ?: variants.mapNotNull { it.mapping?.season }.distinct().singleOrNull()

                    season?.let {
                        append("/season-$it")
                        if (mappings.size == 1) {
                            val episode = mappings.first()
                            append("/${episode.episodeType!!.slug}-${episode.number}")
                        }
                    }
                },
                variants.map { LangType.fromAudioLocale(countryCode, it.audioLocale!!) }.toTreeSet(),
                element.episodeType,
                mappings.minOfOrNull { it.number!! },
                mappings.maxOfOrNull { it.number!! },
                mappings.firstOrNull()?.number,
                mappings.map { episodeMappingFactory.toDto(it, false) }.toSet()
            )
        }

        return (0..6).map { dayOffset ->
            val date = startOfWeekDay.plusDays(dayOffset.toLong())
            val tuplesDay = releases.filter {
                ZonedDateTime.parse(it.releaseDateTime).withZoneSameInstant(zoneId).dayOfWeek.value == dayOffset + 1
            }

            WeeklyAnimesDto(
                date.format(dayCountryPattern).capitalizeWords(),
                tuplesDay.toSortedSet(
                    compareBy(
                        { ZonedDateTime.parse(it.releaseDateTime).withZoneSameInstant(zoneId).toLocalTime() },
                        { it.anime.shortName }
                    )
                )
            )
        }
    }

    suspend fun addSimulcastToAnime(anime: Anime, simulcast: Simulcast): Boolean {
        if (anime.simulcasts.none { it.uuid == simulcast.uuid }) {
            simulcast.uuid ?: simulcastService.save(simulcast)
            anime.simulcasts.add(simulcast)
            return true
        }

        return false
    }

    suspend fun recalculateSimulcasts() {
        val ignoreEpisodeTypes = setOf(EpisodeType.SUMMARY)

        repository.deleteAllWithoutEpisodes()
        episodeMappingService.updateAllReleaseDate()
        repository.updateAllReleaseDate()

        val simulcastRange = configCacheService.getValueAsInt(ConfigPropertyKey.SIMULCAST_RANGE, 1)
        val simulcastRangeDelay = configCacheService.getValueAsInt(ConfigPropertyKey.SIMULCAST_RANGE_DELAY, 3)
        val simulcasts = simulcastService.findAll().toMutableList()

        val groupedAnimes = findAll().groupBy { it.countryCode!! }
        val episodeSimulcasts = mutableMapOf<UUID, UUID>()

        groupedAnimes.forEach { (countryCode, animes) ->
            val groupedMappings = episodeMappingService.findAllSimulcasted(countryCode.locale, ignoreEpisodeTypes)
                .groupBy { it.animeUuid }

            animes.forEach { anime ->
                anime.simulcasts = mutableSetOf()
                val episodeMappings = groupedMappings[anime.uuid] ?: return@forEach

                episodeMappings.forEach { episodeMapping ->
                    val simulcast = episodeVariantService.getSimulcast(
                        simulcastRange,
                        simulcastRangeDelay,
                        anime,
                        episodeMapping,
                        sqlCheck = false,
                        simulcasts = simulcasts
                    )
                    addSimulcastToAnime(anime, simulcast)

                    if (simulcasts.none { it.uuid == simulcast.uuid }) {
                        simulcasts.add(simulcast)
                    }

                    simulcast.uuid?.let { episodeSimulcasts[episodeMapping.mappingUuid] = it }
                }
            }
        }

        updateAll(groupedAnimes.values.flatten())
        episodeMappingService.updateAllSimulcast(episodeSimulcasts)
    }

    override suspend fun save(entity: Anime): Anime {
        entity.simulcasts = entity.simulcasts.map { simulcast ->
            simulcastService.findBySeasonAndYear(simulcast.season!!, simulcast.year!!) ?: simulcastService.save(simulcast)
        }.toMutableSet()

        entity.description = entity.description?.replace("\n", StringUtils.EMPTY_STRING)?.replace("\r", "")
        return super.save(entity)
    }

    override suspend fun delete(entity: Anime) {
        episodeMappingService.findAllByAnime(entity).forEach { episodeMappingService.delete(it) }
        memberFollowAnimeService.findAllByAnime(entity).forEach { memberFollowAnimeService.delete(it) }
        animePlatformService.findAllByAnime(entity).forEach { animePlatformService.delete(it) }
        animeTagService.deleteAll(animeTagService.findAllByAnime(entity.uuid!!))
        entity.simulcasts = mutableSetOf()
        entity.genres = mutableSetOf()
        super.update(entity)
        super.delete(entity)
    }
}