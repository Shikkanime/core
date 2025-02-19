package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.animes.AnimeAlertDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.animes.AnimeError
import fr.shikkanime.dtos.animes.ErrorType
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.dtos.mappings.EpisodeMappingWithoutAnimeDto
import fr.shikkanime.dtos.variants.VariantReleaseDto
import fr.shikkanime.dtos.weekly.WeeklyAnimeDto
import fr.shikkanime.dtos.weekly.WeeklyAnimesDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.repositories.AnimeRepository
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.utils.StringUtils.capitalizeWords
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AnimeService : AbstractService<Anime, AnimeRepository>() {
    @Inject
    private lateinit var animeRepository: AnimeRepository

    @Inject
    private lateinit var simulcastService: SimulcastService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    @Inject
    private lateinit var traceActionService: TraceActionService

    @Inject
    private lateinit var animePlatformService: AnimePlatformService

    override fun getRepository() = animeRepository

    fun findAllBy(
        countryCode: CountryCode?,
        simulcast: Simulcast?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        searchTypes: Array<LangType>?,
        status: Status? = null,
    ) = animeRepository.findAllBy(countryCode, simulcast, sort, page, limit, searchTypes, status)

    fun findAllByName(
        countryCode: CountryCode?,
        name: String,
        page: Int,
        limit: Int,
        searchTypes: Array<LangType>?
    ): Pageable<Anime> {
        return if (name.length == 1) {
            animeRepository.findAllByFirstLetterCategory(countryCode, name, page, limit, searchTypes)
        } else {
            animeRepository.findAllByName(countryCode, name, page, limit, searchTypes)
        }
    }

    fun findAllUuidAndSlug() = animeRepository.findAllUuidAndSlug()

    fun findAllNeedUpdate(lastDateTime: ZonedDateTime) = animeRepository.findAllNeedUpdate(lastDateTime)

    fun findAllAudioLocales() = animeRepository.findAllAudioLocales()

    fun findAllSeasons() = animeRepository.findAllSeasons()

    fun preIndex() = animeRepository.preIndex()

    fun findBySlug(countryCode: CountryCode, slug: String) = animeRepository.findBySlug(countryCode, slug)

    fun findByName(countryCode: CountryCode, name: String?) =
        animeRepository.findByName(countryCode, name)

    fun getWeeklyAnimes(
        countryCode: CountryCode,
        member: Member?,
        startOfWeekDay: LocalDate
    ): List<WeeklyAnimesDto> {
        val zoneId = ZoneId.of(countryCode.timezone)
        val dayCountryPattern = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag(countryCode.locale))
        val endOfWeekDay = startOfWeekDay.atEndOfWeek()
        val weekRange = startOfWeekDay..endOfWeekDay

        val variantReleaseDtos = episodeVariantCacheService.findAllVariantReleases(
            countryCode,
            member,
            startOfWeekDay,
            zoneId
        )

        val releases = processReleases(variantReleaseDtos, zoneId, weekRange).let { releases ->
            releases.filterNot { isReleaseInCurrentWeek(it, releases, weekRange) }
        }

        return groupAndSortReleases(startOfWeekDay, releases.map { it.first }, zoneId, dayCountryPattern)
    }

    private fun processReleases(
        variantReleaseDtos: List<VariantReleaseDto>,
        zoneId: ZoneId,
        weekRange: ClosedRange<LocalDate>
    ): List<Triple<WeeklyAnimeDto, Int, EpisodeType>> {
        val isCurrentWeek: (VariantReleaseDto) -> Boolean = { it.releaseDateTime.withZoneSameInstant(zoneId).toLocalDate() in weekRange }
        val dayPattern = DateTimeFormatter.ofPattern("EEEE")
        val hourPattern = DateTimeFormatter.ofPattern("HH")

        return variantReleaseDtos.groupBy { variantReleaseDto ->
            variantReleaseDto.releaseDateTime.format(dayPattern) to variantReleaseDto.anime
        }.flatMap { (pair, values) ->
            values.groupBy { variantReleaseDto ->
                variantReleaseDto.releaseDateTime.format(hourPattern)
            }.flatMap { (_, hourValues) ->
                val filter = hourValues.filter(isCurrentWeek)
                createWeeklyAnimeDtos(filter, hourValues, pair)
            }
        }
    }

    private fun createWeeklyAnimeDtos(
        filter: List<VariantReleaseDto>,
        hourValues: List<VariantReleaseDto>,
        pair: Pair<String, Anime>
    ): List<Triple<WeeklyAnimeDto, Int, EpisodeType>> {
        val mappings = filter.asSequence()
            .map { it.episodeMapping }
            .distinctBy { it.uuid }
            .sortedWith(compareBy({ it.releaseDateTime }, { it.season }, { it.episodeType }, { it.number }))
            .toSet()

        return mappings.groupBy { it.episodeType }
            .ifEmpty { mapOf(null to mappings) }
            .map { (episodeType, episodeMappings) ->
                createWeeklyAnimeDto(filter, hourValues, pair, episodeType, episodeMappings.toSet())
            }
    }

    private fun createWeeklyAnimeDto(
        filter: List<VariantReleaseDto>,
        hourValues: List<VariantReleaseDto>,
        pair: Pair<String, Anime>,
        episodeType: EpisodeType?,
        episodeMappings: Set<EpisodeMapping>
    ): Triple<WeeklyAnimeDto, Int, EpisodeType> {
        val platforms = filter.map { it.platform }.ifEmpty { hourValues.map { it.platform } }.sortedBy { it.name }.toSet()
        val releaseDateTime = filter.minOfOrNull { it.releaseDateTime } ?: hourValues.minOf { it.releaseDateTime }
        val langTypes = filter.map {
            LangType.fromAudioLocale(pair.second.countryCode!!, it.audioLocale)
        }
            .sorted()
            .ifEmpty {
                hourValues.map { LangType.fromAudioLocale(pair.second.countryCode!!, it.audioLocale) }
                    .sorted()
            }.toSet()

        return Triple(
            WeeklyAnimeDto(
                AbstractConverter.convert(pair.second, AnimeDto::class.java),
                AbstractConverter.convert(platforms, PlatformDto::class.java)!!,
                releaseDateTime.withUTCString(),
                buildString {
                    append("/animes/${pair.second.slug}")
                    episodeMappings.firstOrNull()?.let {
                        append("/season-${it.season}")
                        if (episodeMappings.size <= 1) append("/${it.episodeType!!.slug}-${it.number}")
                    }
                },
                langTypes,
                episodeType,
                episodeMappings.minOfOrNull { it.number!! },
                episodeMappings.maxOfOrNull { it.number!! },
                episodeMappings.firstOrNull()?.number,
                AbstractConverter.convert(
                    episodeMappings.takeIf { it.isNotEmpty() },
                    EpisodeMappingWithoutAnimeDto::class.java
                )
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
        val toLocalTime = withZoneSameInstant.toLocalTime()
        val closedRange = toLocalTime.minusHours(1)..toLocalTime.plusHours(1)

        if (withZoneSameInstant.toLocalDate() in weekRange) {
            return false
        }

        if (weeklyAnimeDto.second > 5 || weeklyAnimeDto.third == EpisodeType.FILM) {
            return true
        }

        return releases.associateWith { ZonedDateTime.parse(it.first.releaseDateTime).withUTC() }
            .filter { (it, releaseDateTime) ->
                it != weeklyAnimeDto &&
                        releaseDateTime.toLocalDate() in weekRange &&
                        releaseDateTime.dayOfWeek == withZoneSameInstant.dayOfWeek &&
                        it.first.anime.uuid == weeklyAnimeDto.first.anime.uuid &&
                        it.first.langTypes == weeklyAnimeDto.first.langTypes
            }
            .any { it.value.toLocalTime() in closedRange }
    }

    private fun groupAndSortReleases(
        startOfWeekDay: LocalDate,
        releases: List<WeeklyAnimeDto>,
        zoneId: ZoneId,
        dateFormatter: DateTimeFormatter
    ): List<WeeklyAnimesDto> {
        return (0..6).map { dayOffset ->
            val date = startOfWeekDay.plusDays(dayOffset.toLong())
            val tuplesDay = releases.filter {
                ZonedDateTime.parse(it.releaseDateTime).withZoneSameInstant(zoneId).dayOfWeek.value == dayOffset + 1
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

    fun getAlerts(page: Int, limit: Int): PageableDto<AnimeAlertDto> {
        val allSeasons = findAllSeasons()
        val allAudioLocales = findAllAudioLocales()
        val invalidAnimes = mutableMapOf<Anime, Pair<ZonedDateTime, MutableSet<AnimeError>>>()

        findAll().forEach { anime ->
            val animeSeasons = allSeasons[anime.uuid!!] ?: return@forEach

            animeSeasons.map { it.key }.toSortedSet().zipWithNext().forEach { (current, next) ->
                if (current + 1 != next) {
                    invalidAnimes.getOrPut(anime) { (animeSeasons[current] ?: ZonedDateTime.now()) to mutableSetOf() }.second
                        .add(AnimeError(ErrorType.INVALID_CHAIN_SEASON, "$current -> $next"))
                }
            }
        }

        episodeMappingService.findAll()
            .asSequence()
            .sortedWith(
                compareBy(
                    { it.releaseDateTime },
                    { it.season },
                    { it.episodeType },
                    { it.number }
                )
            ).groupBy { "${it.anime!!.uuid!!}${it.season}${it.episodeType}" }
            .values.forEach { episodes ->
                val anime = episodes.first().anime!!
                val audioLocales = allAudioLocales[anime.uuid!!] ?: return@forEach

                if (episodes.first().episodeType == EpisodeType.EPISODE) {
                    episodes.groupBy { it.releaseDateTime.toLocalDate() }
                        .values.forEach {
                            if (it.size > 3 && !(audioLocales.size == 1 && LangType.fromAudioLocale(anime.countryCode!!, audioLocales.first()) == LangType.VOICE)) {
                                it.forEach { episodeMapping ->
                                    invalidAnimes.getOrPut(episodeMapping.anime!!) { episodeMapping.releaseDateTime to mutableSetOf() }.second
                                        .add(AnimeError(ErrorType.INVALID_RELEASE_DATE, "S${episodeMapping.season} ${episodeMapping.releaseDateTime.toLocalDate()}[${it.size}]"))
                                    return@forEach
                                }
                            }
                        }
                }

                episodes.filter { it.number!! < 0 }
                    .forEach { episodeMapping ->
                        invalidAnimes.getOrPut(episodeMapping.anime!!) { episodeMapping.releaseDateTime to mutableSetOf() }.second
                            .add(AnimeError(ErrorType.INVALID_EPISODE_NUMBER, StringUtils.toEpisodeMappingString(AbstractConverter.convert(episodeMapping,
                                EpisodeMappingDto::class.java))))
                    }

                episodes.zipWithNext().forEach { (current, next) ->
                    if (current.number!! + 1 != next.number!!) {
                        invalidAnimes.getOrPut(current.anime!!) { current.releaseDateTime to mutableSetOf() }.second
                            .add(AnimeError(ErrorType.INVALID_CHAIN_EPISODE_NUMBER, "${StringUtils.toEpisodeMappingString(AbstractConverter.convert(current,
                                EpisodeMappingDto::class.java))} -> ${StringUtils.toEpisodeMappingString(AbstractConverter.convert(next,
                                EpisodeMappingDto::class.java))}"))
                    }
                }
            }

        return PageableDto(
            data = invalidAnimes.asSequence()
                .map { (anime, pair) ->
                    AnimeAlertDto(
                        AbstractConverter.convert(anime, AnimeDto::class.java),
                        pair.first.withUTCString(),
                        pair.second
                    )
                }.sortedByDescending { ZonedDateTime.parse(it.zonedDateTime) }
                .drop((page - 1) * limit)
                .take(limit)
                .toSet(),
            page = page,
            limit = limit,
            total = invalidAnimes.size.toLong()
        )
    }

    fun addImage(uuid: UUID, image: String, bypass: Boolean = false) {
        ImageService.add(uuid, ImageService.Type.IMAGE, image, 480, 720, bypass)
    }

    fun addBanner(uuid: UUID, image: String, bypass: Boolean = false) {
        ImageService.add(uuid, ImageService.Type.BANNER, image, 640, 360, bypass)
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
        val ignoreEpisodeTypes = setOf(EpisodeType.FILM, EpisodeType.SUMMARY)

        episodeMappingService.updateAllReleaseDate()
        animeRepository.updateAllReleaseDate()

        val mappingsGroupped = episodeVariantService.findAll()
            .asSequence()
            .filter { it.mapping!!.episodeType !in ignoreEpisodeTypes && it.audioLocale != it.mapping!!.anime!!.countryCode!!.locale }
            .mapNotNull { it.mapping }
            .distinctBy { it.uuid }
            .sortedWith(compareBy({ it.releaseDateTime }, { it.season }, { it.episodeType }, { it.number }))
            .groupBy { it.anime!!.uuid!! }

        val animes = findAll()

        animes.forEach { anime ->
            // Avoid lazy loading exception
            anime.simulcasts = mutableSetOf()

            val episodeMappings = mappingsGroupped[anime.uuid] ?: return@forEach

            episodeMappings.forEach { episodeMapping ->
                val previousReleaseDateTime = episodeMappings.filter {
                    it.releaseDateTime < episodeMapping.releaseDateTime &&
                            it.episodeType == episodeMapping.episodeType
                }.maxOfOrNull { it.releaseDateTime }

                val simulcast = episodeVariantService.getSimulcast(anime, episodeMapping, previousReleaseDateTime)
                addSimulcastToAnime(anime, simulcast)
            }
        }

        updateAll(animes)
    }

    override fun save(entity: Anime): Anime {
        entity.simulcasts = entity.simulcasts.map { simulcast ->
            simulcastService.findBySeasonAndYear(simulcast.season!!, simulcast.year!!) ?: simulcastService.save(
                simulcast
            )
        }.toMutableSet()

        entity.description = entity.description?.replace("\n", "")
            ?.replace("\r", "")
        entity.status = StringUtils.getStatus(entity)
        val savedEntity = super.save(entity)
        val uuid = savedEntity.uuid!!

        if (!Constant.disableImageConversion) {
            addImage(uuid, savedEntity.image!!)
            addBanner(uuid, savedEntity.banner!!)
        }

        traceActionService.createTraceAction(entity, TraceAction.Action.CREATE)
        return savedEntity
    }

    fun update(uuid: UUID, animeDto: AnimeDto): Anime? {
        val anime = find(uuid) ?: return null

        if (animeDto.name.isNotBlank() && animeDto.name != anime.name) {
            anime.name = animeDto.name
        }

        if (animeDto.slug.isNotBlank() && animeDto.slug != anime.slug) {
            val findBySlug = findBySlug(anime.countryCode!!, animeDto.slug)

            if (findBySlug != null && findBySlug.uuid != anime.uuid) {
                return merge(anime, findBySlug)
            }

            anime.slug = animeDto.slug
        }

        if (!animeDto.description.isNullOrBlank() && animeDto.description != anime.description) {
            anime.description = animeDto.description
        }

        if (animeDto.image.isNotBlank() && animeDto.image != anime.image) {
            anime.image = animeDto.image
            addImage(anime.uuid!!, anime.image!!, true)
        }

        if (animeDto.banner.isNotBlank() && animeDto.banner != anime.banner) {
            anime.banner = animeDto.banner
            addBanner(anime.uuid!!, anime.banner!!, true)
        }

        updateAnimeSimulcast(animeDto, anime)

        anime.status = StringUtils.getStatus(anime)
        val update = super.update(anime)
        traceActionService.createTraceAction(anime, TraceAction.Action.UPDATE)
        return update
    }

    private fun updateAnimeSimulcast(animeDto: AnimeDto, anime: Anime) {
        anime.simulcasts.clear()

        animeDto.simulcasts?.forEach { simulcastDto ->
            val simulcast = simulcastService.find(simulcastDto.uuid!!) ?: return@forEach

            if (anime.simulcasts.none { it.uuid == simulcast.uuid }) {
                anime.simulcasts.add(simulcast)
            }
        }
    }

    override fun delete(entity: Anime) {
        episodeMappingService.findAllByAnime(entity).forEach { episodeMappingService.delete(it) }
        memberFollowAnimeService.findAllByAnime(entity).forEach { memberFollowAnimeService.delete(it) }
        animePlatformService.findAllByAnime(entity).forEach { animePlatformService.delete(it) }
        super.delete(entity)
        traceActionService.createTraceAction(entity, TraceAction.Action.DELETE)
    }

    fun merge(from: Anime, to: Anime): Anime {
        episodeMappingService.findAllByAnime(from).forEach { episodeMapping ->
            val findByAnimeSeasonEpisodeTypeNumber = episodeMappingService.findByAnimeSeasonEpisodeTypeNumber(
                to.uuid!!,
                episodeMapping.season!!,
                episodeMapping.episodeType!!,
                episodeMapping.number!!
            )

            if (findByAnimeSeasonEpisodeTypeNumber != null) {
                episodeVariantService.findAllByMapping(episodeMapping).forEach { episodeVariant ->
                    episodeVariant.mapping = findByAnimeSeasonEpisodeTypeNumber
                    episodeVariantService.update(episodeVariant)
                }

                episodeMappingService.delete(episodeMapping)
                return@forEach
            }

            episodeMapping.anime = to
            episodeMappingService.update(episodeMapping)
        }

        memberFollowAnimeService.findAllByAnime(from).forEach { memberFollowAnime ->
            if (memberFollowAnimeService.existsByMemberAndAnime(memberFollowAnime.member!!, to)) {
                memberFollowAnimeService.delete(memberFollowAnime)
            } else {
                memberFollowAnime.anime = to
                memberFollowAnimeService.update(memberFollowAnime)
            }
        }

        animePlatformService.findAllByAnime(from).forEach { animePlatform ->
            val findByAnimePlatformAndId =
                animePlatformService.findByAnimePlatformAndId(to, animePlatform.platform!!, animePlatform.platformId!!)

            if (findByAnimePlatformAndId != null) {
                animePlatformService.delete(animePlatform)
                return@forEach
            }

            animePlatform.anime = to
            animePlatformService.update(animePlatform)
        }

        delete(from)
        return to
    }
}