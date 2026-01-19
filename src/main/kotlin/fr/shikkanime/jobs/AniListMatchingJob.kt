package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.*
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.*
import fr.shikkanime.wrappers.factories.AbstractAniListWrapper
import fr.shikkanime.wrappers.impl.caches.AniListCachedWrapper
import java.time.ZonedDateTime
import java.util.*
import java.util.logging.Logger

class AniListMatchingJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var simulcastCacheService: SimulcastCacheService
    @Inject private lateinit var animePlatformService: AnimePlatformService
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var mailService: MailService
    @Inject private lateinit var genreService: GenreService
    @Inject private lateinit var animeTagService: AnimeTagService
    @Inject private lateinit var tagService: TagService

    private fun Logger.info(stringBuilder: StringBuilder, message: String) {
        this.info(message)
        stringBuilder.appendLine(message)
    }

    private fun Logger.warning(stringBuilder: StringBuilder, message: String) {
        this.warning(message)
        stringBuilder.appendLine(message)
    }

    private fun getSimulcastUuids(): List<UUID>? {
        val allUuids = simulcastCacheService.findAll().mapNotNull(SimulcastDto::uuid)
        val matchingSize = configCacheService.getValueAsInt(ConfigPropertyKey.ANILIST_SIMULCAST_MATCHING_SIZE, 1)

        return allUuids
            .let { if (matchingSize > 0) it.take(matchingSize) else it }
            .takeIfNotEmpty()
    }

    override suspend fun run() {
        val stringBuilder = StringBuilder()
        val zonedDateTime = ZonedDateTime.now().withSecond(0).withNano(0).withUTC()
        val simulcastUuids = getSimulcastUuids() ?: return

        val animes = CountryCode.entries.flatMap { countryCode ->
            animeService.findAllSimulcastedWithAnimePlatformInvalid(
                simulcastUuids,
                Platform.ANIL,
                zonedDateTime.minusDays(configCacheService.getValueAsInt(ConfigPropertyKey.MATCHING_ANILIST_DELAY, 30).toLong()),
                countryCode.locale
            )
        }.distinctBy { it.uuid }

        logger.info(stringBuilder, "Found ${animes.size} animes to match")

        if (animes.isEmpty()) {
            logger.info(stringBuilder, "No animes found")
            return
        }

        val deprecatedAnimePlatformDateTime = zonedDateTime.minusMonths(configCacheService.getValueAsInt(ConfigPropertyKey.ANIME_PLATFORM_DEPRECATED_DURATION, 3).toLong())
        val needMatchingAnimes = animes.shuffled().take(configCacheService.getValueAsInt(ConfigPropertyKey.MATCHING_ANILIST_ANIME_SIZE, 5))
        var hasChange = false

        needMatchingAnimes.forEach { anime ->
            val shortName = StringUtils.getShortName(anime.name!!)
            logger.info(stringBuilder, "Matching anime $shortName...")

            val animePlatforms = animePlatformService.findAllByAnime(anime)
            val streamingPlatforms = animePlatforms.filter { it.platform!!.isStreamingPlatform }

            val anilistPlatforms = animePlatforms.filter { it.platform!! == Platform.ANIL }
            val media = AniListCachedWrapper.findAnilistMedia(anime.name!!, streamingPlatforms, anime.releaseDateTime.year)

            if (media == null) {
                logger.warning(stringBuilder, "No AniList media found for $shortName")
                deleteDeprecatedPlatforms(stringBuilder, shortName, anilistPlatforms, deprecatedAnimePlatformDateTime).onTrue { hasChange = true }

                if (updateAnimeGenreAndTags(stringBuilder, anime, shortName, null)) {
                    hasChange = true
                    animeService.update(anime)
                }

                return@forEach
            }

            logger.info(stringBuilder, "AniList media found with ID: ${media.id}")

            anilistPlatforms.singleOrNull { it.platformId == media.id.toString() }?.let {
                logger.info(stringBuilder, "Anime $shortName is already matched with AniList ID ${media.id}, validating...")
                it.lastValidateDateTime = zonedDateTime
                animePlatformService.update(it)
                traceActionService.createTraceAction(it, TraceAction.Action.UPDATE)
                return@forEach
            }

            logger.info(stringBuilder, "Anime $shortName has no match with AniList ID ${media.id}, creating it...")

            animePlatformService.save(AnimePlatform(
                anime = anime,
                platform = Platform.ANIL,
                platformId = media.id.toString(),
                lastValidateDateTime = zonedDateTime,
            )).apply {
                traceActionService.createTraceAction(this, TraceAction.Action.CREATE)
                hasChange = true
            }

            deleteDeprecatedPlatforms(stringBuilder, shortName, anilistPlatforms, deprecatedAnimePlatformDateTime).onTrue { hasChange = true }

            if (updateAnimeGenreAndTags(stringBuilder, anime, shortName, media)) {
                hasChange = true
                animeService.update(anime)
            }
        }

        logger.info(stringBuilder, "Matching job finished")

        if (hasChange) {
            mailService.saveAdminMail(
                title = "AniListMatchingJob - ${needMatchingAnimes.size} animes matched",
                body = stringBuilder.toString().replace("\n", "<br>")
            )

            InvalidationService.invalidate(AnimePlatform::class.java, Anime::class.java, Genre::class.java, AnimeTag::class.java, Tag::class.java)
        }
    }

    private fun deleteDeprecatedPlatforms(
        stringBuilder: StringBuilder,
        shortName: String,
        anilistPlatforms: List<AnimePlatform>,
        zonedDateTime: ZonedDateTime,
    ): Boolean {
        var hasDeleted = false

        anilistPlatforms.filter { it.lastValidateDateTime != null && it.lastValidateDateTime!!.isBeforeOrEqual(zonedDateTime) }
            .forEach {
                logger.warning(stringBuilder, "Deleting old anime platform ${it.platform} for anime $shortName with id ${it.platformId}")
                animePlatformService.delete(it)
                traceActionService.createTraceAction(it, TraceAction.Action.DELETE)
                hasDeleted = true
            }

        return hasDeleted
    }

    private fun updateAnimeGenreAndTags(stringBuilder: StringBuilder, anime: Anime, shortName: String, media: AbstractAniListWrapper.Media?): Boolean {
        val currentGenres = genreService.findAllByAnime(anime.uuid!!)
        val currentAnimeTags = animeTagService.findAllByAnime(anime.uuid)

        if (media == null) {
            if (currentGenres.isNotEmpty() || currentAnimeTags.isNotEmpty()) {
                logger.warning(stringBuilder, "Anime $shortName has no AniList entry, but has genres or tags, removing them...")
                anime.genres = mutableSetOf()
                animeTagService.deleteAll(currentAnimeTags)
                return true
            }

            return false
        }

        val genresChanged = updateGenres(stringBuilder, anime, media.genres.orEmpty(), currentGenres, shortName)
        val tagsChanged = updateTags(stringBuilder, anime, media.tags.orEmpty(), currentAnimeTags, shortName)

        return genresChanged || tagsChanged
    }

    private fun updateGenres(stringBuilder: StringBuilder, anime: Anime, mediaGenres: List<String>, currentGenres: List<Genre>, shortName: String): Boolean {
        val sortedMediaGenres = mediaGenres.sortedBy { it.lowercase() }
        val sortedCurrentGenres = currentGenres.mapNotNull { it.name }.sortedBy { it.lowercase() }

        if (sortedMediaGenres == sortedCurrentGenres) return false

        anime.genres = sortedMediaGenres.map(genreService::findOrSave).toMutableSet()
        logger.info(stringBuilder, "Genres updated for anime $shortName to ${sortedMediaGenres.joinToString()}")
        return true
    }

    private fun updateTags(stringBuilder: StringBuilder, anime: Anime, mediaTags: List<AbstractAniListWrapper.Tag>, currentAnimeTags: List<AnimeTag>, shortName: String): Boolean {
        val filteredMediaTags = mediaTags.filter { it.rank >= 75 }.sortedBy { it.name.lowercase() }
        val sortedMediaTagNames = filteredMediaTags.map { it.name.lowercase() }
        val sortedCurrentTagNames = currentAnimeTags.mapNotNull { it.tag?.name?.lowercase() }.sortedBy { it }

        if (sortedMediaTagNames == sortedCurrentTagNames) {
            return updateExistingTagsMetadata(filteredMediaTags, currentAnimeTags)
        }

        val targetTags = filteredMediaTags.map { tagService.findOrSave(it.name) }
        val targetTagUuids = targetTags.mapNotNull { it.uuid }.toSet()

        // Remove tags not in AniList anymore
        val tagsToDelete = currentAnimeTags.filter { it.tag?.uuid !in targetTagUuids }
        animeTagService.deleteAll(tagsToDelete)

        // Add or Update remaining tags
        filteredMediaTags.forEach { mediaTag ->
            val tag = targetTags.find { it.name == mediaTag.name } ?: return@forEach
            val existingAnimeTag = currentAnimeTags.find { it.tag?.uuid == tag.uuid }
            val isSpoiler = mediaTag.isMediaSpoiler || mediaTag.isGeneralSpoiler

            if (existingAnimeTag == null) {
                animeTagService.saveAll(listOf(AnimeTag(anime = anime, tag = tag, isAdult = mediaTag.isAdult, isSpoiler = isSpoiler)))
            } else if (existingAnimeTag.isAdult != mediaTag.isAdult || existingAnimeTag.isSpoiler != isSpoiler) {
                existingAnimeTag.isAdult = mediaTag.isAdult
                existingAnimeTag.isSpoiler = isSpoiler
                animeTagService.update(existingAnimeTag)
            }
        }

        logger.info(stringBuilder, "Tags updated for anime $shortName to ${filteredMediaTags.joinToString { it.name }}")
        return true
    }

    private fun updateExistingTagsMetadata(mediaTags: List<AbstractAniListWrapper.Tag>, currentAnimeTags: List<AnimeTag>): Boolean {
        var hasChanged = false
        currentAnimeTags.forEach { animeTag ->
            val mediaTag = mediaTags.find { it.name == animeTag.tag?.name } ?: return@forEach
            val isSpoiler = mediaTag.isMediaSpoiler || mediaTag.isGeneralSpoiler
            if (animeTag.isAdult != mediaTag.isAdult || animeTag.isSpoiler != isSpoiler) {
                animeTag.isAdult = mediaTag.isAdult
                animeTag.isSpoiler = isSpoiler
                animeTagService.update(animeTag)
                hasChanged = true
            }
        }
        return hasChanged
    }
}