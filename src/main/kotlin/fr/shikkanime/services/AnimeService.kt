package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.WeeklyAnimeDto
import fr.shikkanime.dtos.WeeklyAnimesDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingWithoutAnimeDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.repositories.AnimeRepository
import fr.shikkanime.utils.*
import fr.shikkanime.utils.StringUtils.capitalizeWords
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
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
        searchTypes: List<LangType>?,
        status: Status? = null,
    ) = animeRepository.findAllBy(countryCode, simulcast, sort, page, limit, searchTypes, status)

    fun findAllByName(
        countryCode: CountryCode?,
        name: String,
        page: Int,
        limit: Int,
        searchTypes: List<LangType>?
    ): Pageable<Anime> {
        return if (name.length == 1) {
            animeRepository.findAllByFirstLetterCategory(countryCode, name, page, limit, searchTypes)
        } else {
            animeRepository.findAllByName(countryCode, name, page, limit, searchTypes)
        }
    }

    fun findAllUuidImageAndBanner() = animeRepository.findAllUuidImageAndBanner()

    fun preIndex() = animeRepository.preIndex()

    fun findLoaded(uuid: UUID) = animeRepository.findLoaded(uuid)

    fun findByName(countryCode: CountryCode, name: String?) =
        animeRepository.findByName(countryCode, name)

    fun findBySlug(countryCode: CountryCode, slug: String) = animeRepository.findBySlug(countryCode, slug)

    fun getWeeklyAnimes(countryCode: CountryCode, member: Member?, startOfWeekDay: LocalDate): List<WeeklyAnimesDto> {
        val zoneId = ZoneId.of(countryCode.timezone)
        val startOfPreviousWeek = startOfWeekDay.minusWeeks(1).atStartOfDay(zoneId)
        val endOfWeek = startOfWeekDay.atEndOfWeek().atEndOfTheDay(zoneId)

        val tuples = episodeVariantService.findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleByDateRange(
            countryCode, member, startOfPreviousWeek, endOfWeek
        )

        val dateFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag(countryCode.locale))
        val currentWeek = startOfWeekDay[ChronoField.ALIGNED_WEEK_OF_YEAR]

        return (0..6).map { dayOffset ->
            val date = startOfWeekDay.plusDays(dayOffset.toLong())
            val zonedDate = date.atStartOfDay(zoneId)
            val tuplesDay = tuples.filter { (it[2] as ZonedDateTime).withZoneSameInstant(zoneId).dayOfWeek == zonedDate.dayOfWeek }

            WeeklyAnimesDto(
                date.format(dateFormatter).capitalizeWords(),
                tuplesDay.groupBy { triple ->
                    val anime = triple[0] as Anime
                    anime to LangType.fromAudioLocale(anime.countryCode!!, triple[4] as String)
                }.flatMap { (pair, values) ->
                    val (anime, langType) = pair
                    val releaseDateTime = values.maxOf { it[2] as ZonedDateTime }

                    val mappings = values.filter {
                        (it[2] as ZonedDateTime).withZoneSameInstant(zoneId)[ChronoField.ALIGNED_WEEK_OF_YEAR] == currentWeek
                    }.map { it[1] as EpisodeMapping }
                        .distinctBy { it.uuid }
                        .sortedWith(compareBy({ it.releaseDateTime }, { it.season }, { it.episodeType }, { it.number }))

                    mappings.groupBy { it.episodeType }.ifEmpty { mapOf(null to mappings) }.map { (episodeType, episodeMappings) ->
                        WeeklyAnimeDto(
                            AbstractConverter.convert(anime, AnimeDto::class.java),
                            AbstractConverter.convert(values.mapNotNull { it[3] as? Platform }.distinct(), PlatformDto::class.java)!!,
                            releaseDateTime.withUTCString(),
                            buildString {
                                append("/animes/${anime.slug}")
                                episodeMappings.firstOrNull()?.let {
                                    append("/season-${it.season}")
                                    if (mappings.size <= 1) append("/${it.episodeType!!.slug}-${it.number}")
                                }
                            },
                            langType,
                            episodeType,
                            episodeMappings.minOfOrNull { it.number!! },
                            episodeMappings.maxOfOrNull { it.number!! },
                            episodeMappings.firstOrNull()?.number,
                            AbstractConverter.convert(episodeMappings.takeIf { it.isNotEmpty() }, EpisodeMappingWithoutAnimeDto::class.java)
                        )
                    }
                }.sortedWith(
                    compareBy(
                        { ZonedDateTime.parse(it.releaseDateTime).withZoneSameInstant(zoneId).toLocalTime() },
                        { it.anime.shortName },
                        { it.langType }
                    )
                )
            )
        }
    }

    fun addImage(uuid: UUID, image: String, bypass: Boolean = false) {
        ImageService.add(uuid, ImageService.Type.IMAGE, image, 480, 720, bypass)
    }

    fun addBanner(uuid: UUID, image: String, bypass: Boolean = false) {
        ImageService.add(uuid, ImageService.Type.BANNER, image, 640, 360, bypass)
    }

    fun addSimulcastToAnime(anime: Anime, simulcast: Simulcast) {
        if (anime.simulcasts.isEmpty() || anime.simulcasts.none { s -> s.uuid == simulcast.uuid }) {
            if (simulcast.uuid == null) {
                simulcastService.save(simulcast)
                MapCache.invalidate(Simulcast::class.java)
            }

            anime.simulcasts.add(simulcast)
        }
    }

    fun recalculateSimulcasts() {
        episodeMappingService.updateAllReleaseDate()
        animeRepository.updateAllReleaseDate()

        findAll().forEach { anime ->
            // Avoid lazy loading exception
            anime.simulcasts = mutableSetOf()

            val variants = episodeVariantService.findAllSimulcastedByAnime(anime)

            val episodeMappings = variants.mapNotNull { it.mapping }.distinctBy { it.uuid }
                .sortedWith(compareBy({ it.releaseDateTime }, { it.season }, { it.episodeType }, { it.number }))

            episodeMappings.forEach { episodeMapping ->
                val previousReleaseDateTime = variants.filter {
                    it.mapping!!.anime!!.uuid == anime.uuid &&
                            it.mapping!!.releaseDateTime.isBefore(episodeMapping.releaseDateTime) &&
                            it.mapping!!.episodeType == episodeMapping.episodeType &&
                            it.audioLocale != anime.countryCode!!.locale
                }.maxOfOrNull { it.mapping!!.releaseDateTime }

                val simulcast = episodeVariantService.getSimulcast(anime, episodeMapping, previousReleaseDateTime)
                addSimulcastToAnime(anime, simulcast)
            }

            update(anime)
        }
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
        addImage(uuid, savedEntity.image!!)
        addBanner(uuid, savedEntity.banner!!)
        MapCache.invalidate(Anime::class.java)
        traceActionService.createTraceAction(entity, TraceAction.Action.CREATE)
        return savedEntity
    }

    fun update(uuid: UUID, animeDto: AnimeDto): Anime? {
        val anime = findLoaded(uuid) ?: return null

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
            ImageService.remove(anime.uuid!!, ImageService.Type.IMAGE)
            addImage(anime.uuid, anime.image!!)
        }

        if (animeDto.banner.isNotBlank() && animeDto.banner != anime.banner) {
            anime.banner = animeDto.banner
            ImageService.remove(anime.uuid!!, ImageService.Type.BANNER)
            addBanner(anime.uuid, anime.banner!!)
        }

        updateAnimeSimulcast(animeDto, anime)

        anime.status = StringUtils.getStatus(anime)
        val update = super.update(anime)
        MapCache.invalidate(Anime::class.java)
        traceActionService.createTraceAction(anime, TraceAction.Action.UPDATE)
        return update
    }

    private fun updateAnimeSimulcast(animeDto: AnimeDto, anime: Anime) {
        if (animeDto.simulcasts != null) {
            anime.simulcasts.clear()

            animeDto.simulcasts.forEach { simulcastDto ->
                val simulcast = simulcastService.find(simulcastDto.uuid!!) ?: return@forEach

                if (anime.simulcasts.none { it.uuid == simulcast.uuid }) {
                    anime.simulcasts.add(simulcast)
                }
            }
        }
    }

    override fun delete(entity: Anime) {
        episodeMappingService.findAllByAnime(entity).forEach { episodeMappingService.delete(it) }
        memberFollowAnimeService.findAllByAnime(entity).forEach { memberFollowAnimeService.delete(it) }
        animePlatformService.findAllByAnime(entity).forEach { animePlatformService.delete(it) }
        super.delete(entity)
        MapCache.invalidate(Anime::class.java)
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