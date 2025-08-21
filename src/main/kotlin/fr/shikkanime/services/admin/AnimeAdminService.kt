package fr.shikkanime.services.admin

import com.google.inject.Inject
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.animes.AnimeAlertDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.animes.AnimeError
import fr.shikkanime.dtos.animes.ErrorType
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.factories.impl.AnimeFactory
import fr.shikkanime.services.*
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.withUTCString
import java.time.ZonedDateTime
import java.util.*

class AnimeAdminService : IAdminService {
    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var attachmentService: AttachmentService
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var memberFollowAnimeService: MemberFollowAnimeService
    @Inject private lateinit var animePlatformService: AnimePlatformService

    fun update(uuid: UUID, animeDto: AnimeDto): Anime? {
        val anime = animeService.find(uuid) ?: return null

        if (animeDto.name.isNotBlank() && animeDto.name != anime.name) {
            anime.name = animeDto.name
        }

        if (animeDto.slug.isNotBlank() && animeDto.slug != anime.slug) {
            val findBySlug = animeService.findBySlug(anime.countryCode!!, animeDto.slug)

            if (findBySlug != null && findBySlug.uuid != anime.uuid) {
                return merge(anime, findBySlug)
            }

            anime.slug = animeDto.slug
        }

        if (!animeDto.description.isNullOrBlank() && animeDto.description != anime.description) {
            anime.description = animeDto.description
        }

        if (animeDto.thumbnail.isNullOrBlank().not()) {
            attachmentService.createAttachmentOrMarkAsActive(anime.uuid!!, ImageType.THUMBNAIL, url = animeDto.thumbnail!!)
        }

        if (animeDto.banner.isNullOrBlank().not()) {
            attachmentService.createAttachmentOrMarkAsActive(anime.uuid!!, ImageType.BANNER, url = animeDto.banner!!)
        }

        if (animeDto.carousel.isNullOrBlank().not()) {
            attachmentService.createAttachmentOrMarkAsActive(anime.uuid!!, ImageType.CAROUSEL, url = animeDto.carousel!!)
        }

        val update = animeService.update(anime)
        traceActionService.createTraceAction(anime, TraceAction.Action.UPDATE)
        return update
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
                traceActionService.createTraceAction(animePlatform, TraceAction.Action.DELETE)
                return@forEach
            }

            animePlatform.anime = to
            animePlatformService.update(animePlatform)
            traceActionService.createTraceAction(animePlatform, TraceAction.Action.UPDATE)
        }

        delete(from)
        return to
    }

    fun delete(anime: Anime) {
        animeService.delete(anime)
    }

    fun forceUpdate(uuid: UUID): Anime? {
        val anime = animeService.find(uuid) ?: return null
        anime.lastUpdateDateTime = Constant.oldLastUpdateDateTime
        animeService.update(anime)
        return anime
    }

    fun forceUpdateAll() {
        val animes = animeService.findAll()
        animes.forEach { it.lastUpdateDateTime = Constant.oldLastUpdateDateTime }
        animeService.updateAll(animes)
    }

    fun getAlerts(page: Int, limit: Int): PageableDto<AnimeAlertDto> {
        val invalidAnimes = mutableMapOf<Anime, Pair<ZonedDateTime, MutableSet<AnimeError>>>()

        animeService.findAll().forEach { anime ->
            val seasons = animeService.findAllSeasons(anime.uuid!!)

            seasons.map { it.key }.toSortedSet().zipWithNext().forEach { (current, next) ->
                if (current + 1 != next) {
                    invalidAnimes.getOrPut(anime) { (seasons[current] ?: ZonedDateTime.now()) to mutableSetOf() }.second
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
                val audioLocales = animeService.findAllAudioLocales(anime.uuid!!)

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
                            .add(AnimeError(ErrorType.INVALID_EPISODE_NUMBER, StringUtils.toEpisodeMappingString(episodeMapping)))
                    }

                episodes.zipWithNext().forEach { (current, next) ->
                    if (current.number!! + 1 != next.number!!) {
                        invalidAnimes.getOrPut(current.anime!!) { current.releaseDateTime to mutableSetOf() }.second
                            .add(AnimeError(ErrorType.INVALID_CHAIN_EPISODE_NUMBER, "${StringUtils.toEpisodeMappingString(current)} -> ${StringUtils.toEpisodeMappingString(next)}"))
                    }
                }
            }

        return PageableDto(
            data = invalidAnimes.asSequence()
                .map { (anime, pair) ->
                    AnimeAlertDto(
                        animeFactory.toDto(anime),
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
} 