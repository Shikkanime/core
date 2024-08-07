package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.WeeklyAnimeDto
import fr.shikkanime.dtos.WeeklyAnimesDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.animes.DetailedAnimeDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.repositories.AnimeRepository
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.StringUtils.capitalizeWords
import fr.shikkanime.utils.withUTCString
import java.time.DayOfWeek
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

    override fun getRepository() = animeRepository

    fun findAllLoaded() = animeRepository.findAllLoaded()

    fun findAllBy(
        countryCode: CountryCode?,
        simulcast: Simulcast?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        status: Status? = null,
    ) = animeRepository.findAllBy(countryCode, simulcast, sort, page, limit, status)

    fun findAllByName(name: String, countryCode: CountryCode?, page: Int, limit: Int) =
        animeRepository.findAllByName(name, countryCode, page, limit)

    fun findAllUuidImageAndBanner() = animeRepository.findAllUuidImageAndBanner()

    fun preIndex() = animeRepository.preIndex()

    fun findLoaded(uuid: UUID) = animeRepository.findLoaded(uuid)

    fun findByName(countryCode: CountryCode, name: String?) =
        animeRepository.findByName(countryCode, name)

    fun findBySlug(countryCode: CountryCode, slug: String) = animeRepository.findBySlug(countryCode, slug)

    fun getWeeklyAnimes(member: Member?, startOfWeekDay: LocalDate, countryCode: CountryCode): List<WeeklyAnimesDto> {
        val zoneId = ZoneId.of(countryCode.timezone)
        val startOfPreviousWeek = startOfWeekDay.minusWeeks(1).atStartOfDay(zoneId)
        val endOfWeek = startOfWeekDay.with(DayOfWeek.SUNDAY).atTime(23, 59, 59).atZone(zoneId)

        val tuples = episodeVariantService.findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleByDateRange(
            member, countryCode, startOfPreviousWeek, endOfWeek
        )

        return startOfWeekDay.datesUntil(startOfWeekDay.plusDays(7)).toList().map { date ->
            val zonedDate = date.atStartOfDay(zoneId)
            val tuplesDay = tuples.filter { (it[2] as ZonedDateTime).withZoneSameInstant(zoneId).dayOfWeek == zonedDate.dayOfWeek }

            WeeklyAnimesDto(
                date.format(DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag(countryCode.locale))).capitalizeWords(),
                tuplesDay.groupBy { triple ->
                    val anime = triple[0] as Anime
                    Triple(anime, (triple[1] as EpisodeMapping).episodeType!!, LangType.fromAudioLocale(anime.countryCode!!, triple[4] as String))
                }.map { (triple, values) ->
                    val anime = triple.first
                    val releaseDateTime = values.maxOf { it[2] as ZonedDateTime }

                    val mappings = values.filter {
                        (it[2] as ZonedDateTime).withZoneSameInstant(zoneId)[ChronoField.ALIGNED_WEEK_OF_YEAR] == startOfWeekDay[ChronoField.ALIGNED_WEEK_OF_YEAR]
                    }.map { it[1] as EpisodeMapping }
                        .distinctBy { it.uuid }
                        .sortedWith(compareBy({ it.releaseDateTime }, { it.season }, { it.episodeType }, { it.number }))

                    WeeklyAnimeDto(
                        AbstractConverter.convert(anime, AnimeDto::class.java),
                        AbstractConverter.convert(values.map { it[3] as Platform }.distinct(), PlatformDto::class.java)!!,
                        releaseDateTime.withUTCString(),
                        "/animes/${anime.slug}${
                            mappings.firstOrNull()
                                ?.let { "/season-${it.season}" + ("/${it.episodeType!!.slug}-${it.number}".takeIf { mappings.size <= 1 } ?: "") } ?: ""
                        }",
                        triple.third,
                        mappings.isNotEmpty(),
                        mappings.size > 1,
                        mappings.map { it.uuid!! },
                        mappings.firstOrNull()?.episodeType,
                        mappings.minOfOrNull { it.number!! },
                        mappings.maxOfOrNull { it.number!! },
                        mappings.firstOrNull()?.number
                    )
                }.sortedWith(compareBy({ ZonedDateTime.parse(it.releaseDateTime).withZoneSameInstant(zoneId).toLocalTime() }, { it.anime.shortName }))
            )
        }
    }

    fun addImage(uuid: UUID, image: String, bypass: Boolean = false) {
        ImageService.add(uuid, ImageService.Type.IMAGE, image, 480, 720, bypass)
    }

    fun addBanner(uuid: UUID, image: String?, bypass: Boolean = false) {
        if (image.isNullOrBlank()) return
        ImageService.add(uuid, ImageService.Type.BANNER, image, 640, 360, bypass)
    }

    fun addSimulcastToAnime(anime: Anime, simulcast: Simulcast) {
        if (anime.simulcasts.isEmpty() || anime.simulcasts.none { s -> s.uuid == simulcast.uuid }) {
            if (simulcast.uuid == null) {
                simulcastService.save(simulcast)
            }

            anime.simulcasts.add(simulcast)
        }
    }

    fun recalculateSimulcasts() {
        episodeMappingService.updateAllReleaseDate()
        animeRepository.updateAllReleaseDate()

        findAllLoaded().forEach { anime ->
            anime.simulcasts.clear()

            episodeMappingService.findAllSimulcastedByAnime(anime).forEach { episodeMapping ->
                addSimulcastToAnime(anime, episodeVariantService.getSimulcast(anime, episodeMapping))
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
        addBanner(uuid, savedEntity.banner)
        MapCache.invalidate(Anime::class.java)
        return savedEntity
    }

    fun update(uuid: UUID, detailedAnimeDto: DetailedAnimeDto): Anime? {
        val anime = findLoaded(uuid) ?: return null

        if (detailedAnimeDto.name.isNotBlank() && detailedAnimeDto.name != anime.name) {
            anime.name = detailedAnimeDto.name
        }

        if (!detailedAnimeDto.slug.isNullOrBlank() && detailedAnimeDto.slug != anime.slug) {
            val findBySlug = findBySlug(anime.countryCode!!, detailedAnimeDto.slug!!)

            if (findBySlug != null && findBySlug.uuid != anime.uuid) {
                merge(anime, findBySlug)
            }

            anime.slug = detailedAnimeDto.slug
        }

        if (detailedAnimeDto.releaseDateTime.isNotBlank() && detailedAnimeDto.releaseDateTime != anime.releaseDateTime.toString()) {
            anime.releaseDateTime = ZonedDateTime.parse(detailedAnimeDto.releaseDateTime)
        }

        if (!detailedAnimeDto.image.isNullOrBlank() && detailedAnimeDto.image != anime.image) {
            anime.image = detailedAnimeDto.image
            ImageService.remove(anime.uuid!!, ImageService.Type.IMAGE)
            addImage(anime.uuid, anime.image!!)
        }

        if (!detailedAnimeDto.banner.isNullOrBlank() && detailedAnimeDto.banner != anime.banner) {
            anime.banner = detailedAnimeDto.banner
            ImageService.remove(anime.uuid!!, ImageService.Type.BANNER)
            addBanner(anime.uuid, anime.banner)
        }

        if (!detailedAnimeDto.description.isNullOrBlank() && detailedAnimeDto.description != anime.description) {
            anime.description = detailedAnimeDto.description
        }

        updateAnimeSimulcast(detailedAnimeDto, anime)

        anime.status = StringUtils.getStatus(anime)
        val update = super.update(anime)
        MapCache.invalidate(Anime::class.java)
        return update
    }

    private fun updateAnimeSimulcast(detailedAnimeDto: DetailedAnimeDto, anime: Anime) {
        if (detailedAnimeDto.simulcasts != null) {
            anime.simulcasts.clear()

            detailedAnimeDto.simulcasts.forEach { simulcastDto ->
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
        super.delete(entity)
        MapCache.invalidate(Anime::class.java)
    }

    private fun merge(anime: Anime, findBySlug: Anime) {
        episodeMappingService.findAllByAnime(findBySlug).forEach { episodeMapping ->
            val findByAnimeSeasonEpisodeTypeNumber = episodeMappingService.findByAnimeSeasonEpisodeTypeNumber(
                anime.uuid!!,
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

            episodeMapping.anime = anime
            episodeMappingService.update(episodeMapping)
        }

        memberFollowAnimeService.findAllByAnime(findBySlug).forEach { memberFollowAnime ->
            if (memberFollowAnimeService.existsByMemberAndAnime(memberFollowAnime.member!!, anime)) {
                memberFollowAnimeService.delete(memberFollowAnime)
            } else {
                memberFollowAnime.anime = anime
                memberFollowAnimeService.update(memberFollowAnime)
            }
        }

        delete(findBySlug)
    }
}