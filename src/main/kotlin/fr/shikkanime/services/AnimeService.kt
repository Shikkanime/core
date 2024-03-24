package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.WeeklyAnimeDto
import fr.shikkanime.dtos.WeeklyAnimesDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.repositories.AnimeRepository
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils.capitalizeWords
import io.ktor.http.*
import java.time.LocalDate
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
        return startOfWeekDay.datesUntil(startOfWeekDay.plusDays(7)).map {
            val start = ZonedDateTime.parse("${it}T00:00:00Z")
            val end = ZonedDateTime.parse("${it}T23:59:59Z")
            val dateTitle =
                it.format(DateTimeFormatter.ofPattern("EEEE", Locale.of(countryCode.locale.split("-")[0], countryCode.locale.split("-")[1]))).capitalizeWords()
            val list = episodeService.findAllByDateRange(countryCode, start, end, emptyList()).toMutableList()

            list.addAll(
                episodeService.findAllByDateRange(
                    countryCode,
                    start.minusDays(7),
                    end.minusDays(7),
                    list.mapNotNull { anime -> anime.uuid })
            )

            WeeklyAnimesDto(
                dateTitle,
                AbstractConverter.convert(
                    list.distinctBy { episode -> episode.anime?.uuid },
                    EpisodeDto::class.java
                ).map { episodeDto ->
                    WeeklyAnimeDto(
                        episodeDto.anime,
                        episodeDto.releaseDateTime,
                        AbstractConverter.convert(list.filter { episode -> episode.anime?.uuid == episodeDto.anime.uuid }
                            .map { episode -> episode.platform!! }
                            .distinct(), PlatformDto::class.java)
                    )
                }
            )
        }.toList()
    }

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