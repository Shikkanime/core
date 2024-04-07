package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.WeeklyAnimeDto
import fr.shikkanime.dtos.WeeklyAnimesDto
import fr.shikkanime.dtos.animes.AnimeNoStatusDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.repositories.AnimeRepository
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils.capitalizeWords
import fr.shikkanime.utils.withUTC
import io.ktor.http.*
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
    private lateinit var episodeService: EpisodeService

    override fun getRepository() = animeRepository

    fun findAllBy(
        countryCode: CountryCode?,
        simulcast: UUID?,
        sort: List<SortParameter>,
        page: Int,
        limit: Int
    ) = animeRepository.findAllBy(countryCode, simulcast, sort, page, limit)

    fun preIndex() = animeRepository.preIndex()

    fun findAllByLikeName(countryCode: CountryCode, name: String?) =
        animeRepository.findAllByLikeName(countryCode, name)

    fun findAllByName(name: String, countryCode: CountryCode?, page: Int, limit: Int) =
        animeRepository.findAllByName(name, countryCode, page, limit)

    fun findBySlug(slug: String) = animeRepository.findBySlug(slug)

    fun findAllUUIDAndImage() = animeRepository.findAllUUIDAndImage()

    fun getWeeklyAnimes(startOfWeekDay: LocalDate, countryCode: CountryCode): List<WeeklyAnimesDto> {
        val zoneId = ZoneId.of(countryCode.timezone)
        val start = startOfWeekDay.minusDays(7).atStartOfDay(Constant.utcZoneId)
        val end = startOfWeekDay.plusDays(7).atTime(23, 59, 59).withUTC()
        val list = episodeService.findAllByDateRange(countryCode, start, end)
        val pattern = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag(countryCode.locale))

        return startOfWeekDay.datesUntil(startOfWeekDay.plusDays(7)).toList().map { date ->
            val zonedDate = date.atStartOfDay(zoneId)
            val dateTitle = date.format(pattern).capitalizeWords()
            val episodes =
                list.filter { it.releaseDateTime.withZoneSameInstant(zoneId).dayOfWeek == zonedDate.dayOfWeek }

            WeeklyAnimesDto(
                dateTitle,
                episodes.distinctBy { episode -> episode.anime?.uuid.toString() + episode.langType.toString() }
                    .map { distinctEpisode ->
                        val platforms = episodes.filter { it.anime?.uuid == distinctEpisode.anime?.uuid }
                            .mapNotNull(Episode::platform)
                            .distinct()

                        WeeklyAnimeDto(
                            AbstractConverter.convert(distinctEpisode.anime, AnimeNoStatusDto::class.java).toAnimeDto(),
                            distinctEpisode.releaseDateTime.withUTC().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            distinctEpisode.langType!!,
                            AbstractConverter.convert(platforms, PlatformDto::class.java)
                        )
                    }.sortedBy { ZonedDateTime.parse(it.releaseDateTime).withZoneSameInstant(zoneId).toLocalTime() }
            )
        }
    }

    fun getAllLangTypes(anime: Anime) = animeRepository.getAllLangTypes(anime)

    fun addImage(uuid: UUID, image: String, bypass: Boolean = false) {
        ImageService.add(uuid, ImageService.Type.IMAGE, image, 480, 720, bypass)
    }

    fun addBanner(uuid: UUID, image: String?, bypass: Boolean = false) {
        if (image.isNullOrBlank()) return
        ImageService.add(uuid, ImageService.Type.BANNER, image, 640, 360, bypass)
    }

    override fun save(entity: Anime): Anime {
        entity.simulcasts = entity.simulcasts.map { simulcast ->
            simulcastService.findBySeasonAndYear(simulcast.season!!, simulcast.year!!) ?: simulcastService.save(
                simulcast
            )
        }.toMutableSet()

        val savedEntity = super.save(entity)
        val uuid = savedEntity.uuid!!
        addImage(uuid, savedEntity.image!!)
        addBanner(uuid, savedEntity.banner)
        MapCache.invalidate(Anime::class.java)
        return savedEntity
    }

    fun update(uuid: UUID, parameters: Parameters): Anime? {
        val anime = find(uuid) ?: return null

        parameters["name"]?.takeIf { it.isNotBlank() }?.let { anime.name = it }
        parameters["slug"]?.takeIf { it.isNotBlank() }?.let { anime.slug = it }
        parameters["releaseDateTime"]?.takeIf { it.isNotBlank() }
            ?.let { anime.releaseDateTime = ZonedDateTime.parse("$it:00Z") }

        parameters["image"]?.takeIf { it.isNotBlank() }?.let {
            anime.image = it
            ImageService.remove(anime.uuid!!, ImageService.Type.IMAGE)
            addImage(anime.uuid, anime.image!!)
        }

        parameters["banner"]?.takeIf { it.isNotBlank() }?.let {
            anime.banner = it
            ImageService.remove(anime.uuid!!, ImageService.Type.BANNER)
            addBanner(anime.uuid, anime.banner)
        }

        parameters["description"]?.takeIf { it.isNotBlank() }?.let { anime.description = it }

        val update = super.update(anime)
        MapCache.invalidate(Anime::class.java)
        return update
    }

    override fun delete(entity: Anime) {
        episodeService.findAllByAnime(entity.uuid!!).forEach { episodeService.delete(it) }
        super.delete(entity)
        MapCache.invalidate(Anime::class.java)
    }
}