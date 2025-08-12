package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.variants.VariantReleaseDto
import fr.shikkanime.dtos.weekly.WeeklyAnimeDto
import fr.shikkanime.dtos.weekly.WeeklyAnimesDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.SortParameter
import fr.shikkanime.factories.impl.AnimeFactory
import fr.shikkanime.factories.impl.EpisodeMappingFactory
import fr.shikkanime.factories.impl.PlatformFactory
import fr.shikkanime.repositories.AnimeRepository
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.utils.StringUtils.capitalizeWords
import fr.shikkanime.utils.TelemetryConfig.trace
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AnimeService : AbstractService<Anime, AnimeRepository>() {
    private val tracer = TelemetryConfig.getTracer("AnimeService")
    @Inject private lateinit var animeRepository: AnimeRepository
    @Inject private lateinit var simulcastService: SimulcastService
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var episodeVariantCacheService: EpisodeVariantCacheService
    @Inject private lateinit var memberFollowAnimeService: MemberFollowAnimeService
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var platformFactory: PlatformFactory
    @Inject private lateinit var episodeMappingFactory: EpisodeMappingFactory

    override fun getRepository() = animeRepository

    fun findAllBy(
        countryCode: CountryCode?,
        simulcast: Simulcast?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        searchTypes: Array<LangType>?,
    ) = tracer.trace { animeRepository.findAllBy(countryCode, simulcast, sort, page, limit, searchTypes) }

    fun findAllByName(
        countryCode: CountryCode?,
        name: String,
        page: Int,
        limit: Int,
        searchTypes: Array<LangType>?
    ) = tracer.trace {
        if (name.length == 1) {
            animeRepository.findAllByFirstLetterCategory(countryCode, name, page, limit, searchTypes)
        } else {
            animeRepository.findAllByName(countryCode, name, page, limit, searchTypes)
        }
    }

    fun findAllNeedUpdate(lastDateTime: ZonedDateTime) = animeRepository.findAllNeedUpdate(lastDateTime)

    fun findAllAudioLocales() = animeRepository.findAllAudioLocales()

    fun findAllSeasons() = animeRepository.findAllSeasons()

    fun preIndex() = animeRepository.preIndex()

    fun findBySlug(countryCode: CountryCode, slug: String) = tracer.trace { animeRepository.findBySlug(countryCode, slug) }

    fun findByName(countryCode: CountryCode, name: String?) =
        animeRepository.findByName(countryCode, name)

    fun getWeeklyAnimes(
        countryCode: CountryCode,
        member: Member?,
        startOfWeekDay: LocalDate,
        searchTypes: Array<LangType>? = null,
    ): List<WeeklyAnimesDto> {
        return tracer.trace {
            val zoneId = ZoneId.of(countryCode.timezone)
            val dayCountryPattern = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag(countryCode.locale))
            val endOfWeekDay = startOfWeekDay.atEndOfWeek()
            val weekRange = startOfWeekDay..endOfWeekDay

            val variantReleaseDtos = episodeVariantCacheService.findAllVariantReleases(
                countryCode,
                member,
                startOfWeekDay,
                zoneId,
                searchTypes
            )

            val releases = processReleases(variantReleaseDtos, zoneId, weekRange).let { releases ->
                tracer.trace("AnimeService.isReleaseInCurrentWeek") { releases.filterNot { isReleaseInCurrentWeek(it, releases, weekRange) } }
            }

            groupAndSortReleases(startOfWeekDay, releases.map { it.first }, zoneId, dayCountryPattern)
        }
    }

    private fun isTimeInRange(timeToCheck: LocalTime, referenceTime: LocalTime, toleranceHours: Long): Boolean {
        val minTime = referenceTime.minusHours(toleranceHours)
        val maxTime = referenceTime.plusHours(toleranceHours)

        if (minTime <= maxTime) {
            return timeToCheck >= minTime && timeToCheck <= maxTime
        }

        return timeToCheck >= minTime || timeToCheck <= maxTime
    }

    private fun processReleases(
        variantReleaseDtos: List<VariantReleaseDto>,
        zoneId: ZoneId,
        weekRange: ClosedRange<LocalDate>
    ): List<Triple<WeeklyAnimeDto, Int, EpisodeType>> {
        val groups = mutableMapOf<Pair<String, LocalTime>, UUID>()
        val isCurrentWeek: (VariantReleaseDto) -> Boolean = { it.releaseDateTime.withZoneSameInstant(zoneId).toLocalDate() in weekRange }

        return tracer.trace {
            variantReleaseDtos.filter(isCurrentWeek)
                .map { it.episodeMapping }
                .distinctBy { it.uuid!! }
                .apply { episodeVariantCacheService.findAllByMappings(*this.toTypedArray()) }

            variantReleaseDtos.groupBy { variantReleaseDto ->
                val key = "${variantReleaseDto.releaseDateTime.dayOfWeek}-${variantReleaseDto.anime.uuid}"
                val releaseTime = variantReleaseDto.releaseDateTime.toLocalTime()
                val groupKey = groups.entries.firstOrNull { it.key.first == key && isTimeInRange(releaseTime, it.key.second, 1) }
                groupKey?.value ?: UUID.randomUUID().also { groups[key to releaseTime] = it }
            }.flatMap { (_, hourValues) ->
                val filter = hourValues.filter(isCurrentWeek)
                createWeeklyAnimeDtos(hourValues.first().anime, filter, hourValues)
            }
        }
    }

    private fun createWeeklyAnimeDtos(
        anime: Anime,
        filter: List<VariantReleaseDto>,
        hourValues: List<VariantReleaseDto>
    ): List<Triple<WeeklyAnimeDto, Int, EpisodeType>> {
        val mappings = filter.asSequence()
            .map { it.episodeMapping }
            .distinctBy { it.uuid }
            .sortedWith(compareBy({ it.releaseDateTime }, { it.season }, { it.episodeType }, { it.number }))
            .toSet()

        return mappings.groupBy { it.episodeType }
            .ifEmpty { mapOf(null to mappings) }
            .map { (episodeType, episodeMappings) ->
                createWeeklyAnimeDto(anime, filter, hourValues, episodeType, episodeMappings.toSet())
            }
    }

    private fun createWeeklyAnimeDto(
        anime: Anime,
        filter: List<VariantReleaseDto>,
        hourValues: List<VariantReleaseDto>,
        episodeType: EpisodeType?,
        episodeMappings: Set<EpisodeMapping>
    ): Triple<WeeklyAnimeDto, Int, EpisodeType> {
        val platforms = filter.map { it.platform }.ifEmpty { hourValues.map { it.platform } }.sortedBy { it.name }.toSet()
        val releaseDateTime = filter.minOfOrNull { it.releaseDateTime } ?: hourValues.minOf { it.releaseDateTime }
        val langTypes = filter.map {
            LangType.fromAudioLocale(anime.countryCode!!, it.audioLocale)
        }
            .sorted()
            .ifEmpty {
                hourValues.map { LangType.fromAudioLocale(anime.countryCode!!, it.audioLocale) }
                    .sorted()
            }.toSet()

        return Triple(
            WeeklyAnimeDto(
                animeFactory.toDto(anime),
                platforms.map { platformFactory.toDto(it) }.toSet(),
                releaseDateTime.withUTCString(),
                buildString {
                    append("/animes/${anime.slug}")

                    episodeMappings.takeIf { it.isNotEmpty() }?.let { mappings ->
                        val season = mappings.first().season
                        if (mappings.all { it.season == season }) {
                            append("/season-$season")
                            if (mappings.size == 1) {
                                val episode = mappings.first()
                                append("/${episode.episodeType!!.slug}-${episode.number}")
                            }
                        }
                    } ?: run {
                        val season = hourValues.map { it.episodeMapping.season }.distinct().singleOrNull()
                        season?.let { append("/season-$it") }
                    }
                },
                langTypes,
                episodeType,
                episodeMappings.minOfOrNull { it.number!! },
                episodeMappings.maxOfOrNull { it.number!! },
                episodeMappings.firstOrNull()?.number,
                episodeMappings.takeIf { it.isNotEmpty() }?.map { episodeMappingFactory.toDto(it) }?.toSet()
            ),
            filter.ifEmpty { hourValues }.distinctBy { it.episodeMapping.uuid }.size,
            hourValues.first().episodeMapping.episodeType!!
        )
    }

    private fun isReleaseInCurrentWeek(
        weeklyAnimeDto: Triple<WeeklyAnimeDto, Int, EpisodeType>,
        releases: Collection<Triple<WeeklyAnimeDto, Int, EpisodeType>>,
        weekRange: ClosedRange<LocalDate>
    ): Boolean {
        val withZoneSameInstant = ZonedDateTime.parse(weeklyAnimeDto.first.releaseDateTime).withUTC()

        if (withZoneSameInstant.toLocalDate() in weekRange) {
            return false
        }

        if (weeklyAnimeDto.second > 5 || weeklyAnimeDto.third == EpisodeType.FILM) {
            return true
        }

        return releases.associateWith { ZonedDateTime.parse(it.first.releaseDateTime).withUTC() }
            .any { (it, releaseDateTime) ->
                it != weeklyAnimeDto &&
                        releaseDateTime.toLocalDate() in weekRange &&
                        releaseDateTime.dayOfWeek == withZoneSameInstant.dayOfWeek &&
                        it.first.anime.uuid == weeklyAnimeDto.first.anime.uuid &&
                        it.first.langTypes == weeklyAnimeDto.first.langTypes
            }
    }

    private fun groupAndSortReleases(
        startOfWeekDay: LocalDate,
        releases: List<WeeklyAnimeDto>,
        zoneId: ZoneId,
        dateFormatter: DateTimeFormatter
    ): List<WeeklyAnimesDto> {
        return tracer.trace {
            (1..7).map { dayOffset ->
                val date = startOfWeekDay.plusDays(dayOffset.toLong())

                val tuplesDay = releases.filter {
                    ZonedDateTime.parse(it.releaseDateTime).withZoneSameInstant(zoneId).dayOfWeek.value == dayOffset
                }

                WeeklyAnimesDto(
                    date.format(dateFormatter).capitalizeWords(),
                    tuplesDay.sortedWith(
                        compareBy(
                            { ZonedDateTime.parse(it.releaseDateTime).withZoneSameInstant(zoneId).toLocalTime() },
                            { it.anime.shortName }
                        )
                    ).toSet()
                )
            }
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

                    val simulcast = episodeVariantService.getSimulcast(anime, episodeMapping, previousReleaseDateTime, sqlCheck = false)
                    addSimulcastToAnime(anime, simulcast)
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