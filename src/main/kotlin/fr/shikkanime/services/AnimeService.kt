package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.WeeklyAnimeDto
import fr.shikkanime.dtos.WeeklyAnimesDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.repositories.AnimeRepository
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.StringUtils.capitalizeWords
import fr.shikkanime.utils.withUTCString
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

    override fun getRepository() = animeRepository

    fun findAllBy(
        countryCode: CountryCode?,
        simulcast: Simulcast?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int,
        status: Status? = null,
    ) = animeRepository.findAllBy(countryCode, simulcast, sort, page, limit, status)

    fun preIndex() = animeRepository.preIndex()

    fun findByName(countryCode: CountryCode, name: String?) =
        animeRepository.findByName(countryCode, name)

    fun findAllByName(name: String, countryCode: CountryCode?, page: Int, limit: Int) =
        animeRepository.findAllByName(name, countryCode, page, limit)

    fun findBySlug(slug: String) = animeRepository.findBySlug(slug)

    fun getWeeklyAnimes(startOfWeekDay: LocalDate, countryCode: CountryCode): List<WeeklyAnimesDto> {
        val zoneId = ZoneId.of(countryCode.timezone)
        val start = startOfWeekDay.minusDays(7).atStartOfDay(zoneId)
        val end = startOfWeekDay.plusDays(7).atTime(23, 59, 59).atZone(zoneId)
        val list = episodeVariantService.findAllByDateRange(countryCode, start, end)
        val pattern = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag(countryCode.locale))

        return startOfWeekDay.datesUntil(startOfWeekDay.plusDays(7)).toList().map { date ->
            val zonedDate = date.atStartOfDay(zoneId)
            val dateTitle = date.format(pattern).capitalizeWords()
            val episodeVariants =
                list.filter { it.releaseDateTime.withZoneSameInstant(zoneId).dayOfWeek == zonedDate.dayOfWeek }

            WeeklyAnimesDto(
                dateTitle,
                episodeVariants.distinctBy { episodeVariant ->
                    val anime = episodeVariant.mapping!!.anime!!
                    anime.uuid.toString() + LangType.fromAudioLocale(anime.countryCode!!, episodeVariant.audioLocale!!)
                }.map { distinctVariant ->
                    val anime = distinctVariant.mapping!!.anime!!
                    val platforms = episodeVariants.filter { it.mapping == distinctVariant.mapping }
                        .mapNotNull(EpisodeVariant::platform)
                        .sorted()
                        .distinct()

                    WeeklyAnimeDto(
                        AbstractConverter.convert(anime, AnimeDto::class.java),
                        distinctVariant.releaseDateTime.withUTCString(),
                        LangType.fromAudioLocale(anime.countryCode!!, distinctVariant.audioLocale!!),
                        AbstractConverter.convert(platforms, PlatformDto::class.java)!!
                    )
                }.sortedWith(compareBy({
                    ZonedDateTime.parse(it.releaseDateTime).withZoneSameInstant(zoneId).toLocalTime()
                }, { it.anime.shortName }))
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

    fun update(uuid: UUID, animeDto: AnimeDto): Anime? {
        val anime = find(uuid) ?: return null

        if (animeDto.name.isNotBlank() && animeDto.name != anime.name) {
            anime.name = animeDto.name
        }

        if (!animeDto.slug.isNullOrBlank() && animeDto.slug != anime.slug) {
            anime.slug = animeDto.slug
        }

        if (animeDto.releaseDateTime.isNotBlank() && animeDto.releaseDateTime != anime.releaseDateTime.toString()) {
            anime.releaseDateTime = ZonedDateTime.parse(animeDto.releaseDateTime)
        }

        if (!animeDto.image.isNullOrBlank() && animeDto.image != anime.image) {
            anime.image = animeDto.image
            ImageService.remove(anime.uuid!!, ImageService.Type.IMAGE)
            addImage(anime.uuid, anime.image!!)
        }

        if (!animeDto.banner.isNullOrBlank() && animeDto.banner != anime.banner) {
            anime.banner = animeDto.banner
            ImageService.remove(anime.uuid!!, ImageService.Type.BANNER)
            addBanner(anime.uuid, anime.banner)
        }

        if (!animeDto.description.isNullOrBlank() && animeDto.description != anime.description) {
            anime.description = animeDto.description
        }

        updateAnimeSimulcast(animeDto, anime)

        anime.status = StringUtils.getStatus(anime)
        val update = super.update(anime)
        MapCache.invalidate(Anime::class.java)
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
        entity.mappings.forEach { episodeMappingService.delete(it) }
        super.delete(entity)
        MapCache.invalidate(Anime::class.java)
    }
}