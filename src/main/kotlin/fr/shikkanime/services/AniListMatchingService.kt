package fr.shikkanime.services

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimeTag
import fr.shikkanime.entities.Genre
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.wrappers.factories.AbstractAniListWrapper

object AniListMatchingService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val genreService = Constant.injector.getInstance(GenreService::class.java)
    private val tagService = Constant.injector.getInstance(TagService::class.java)
    private val animeTagService = Constant.injector.getInstance(AnimeTagService::class.java)

    fun updateAnimeGenreAndTags(anime: Anime, shortName: String, media: AbstractAniListWrapper.Media?): Boolean {
        val currentGenres = genreService.findAllByAnime(anime.uuid!!)
        val currentAnimeTags = animeTagService.findAllByAnime(anime.uuid)

        if (media == null) {
            if (currentGenres.isNotEmpty() || currentAnimeTags.isNotEmpty()) {
                logger.warning("Anime $shortName has no AniList entry, but has genres or tags, removing them...")
                anime.genres = mutableSetOf()
                animeTagService.deleteAll(currentAnimeTags)
                return true
            }

            return false
        }

        val genresChanged = updateGenres(anime, media.genres.orEmpty(), currentGenres, shortName)
        val tagsChanged = updateTags(anime, media.tags.orEmpty(), currentAnimeTags, shortName)

        return genresChanged || tagsChanged
    }

    private fun updateGenres(
        anime: Anime,
        mediaGenres: List<String>,
        currentGenres: List<Genre>,
        shortName: String
    ): Boolean {
        val sortedMediaGenres = mediaGenres.sortedBy { it.lowercase() }
        val sortedCurrentGenres = currentGenres.mapNotNull { it.name }.sortedBy { it.lowercase() }

        if (sortedMediaGenres == sortedCurrentGenres) return false

        anime.genres = sortedMediaGenres.map(genreService::findOrSave).toMutableSet()
        logger.info("Genres updated for anime $shortName to ${sortedMediaGenres.joinToString()}")
        return true
    }

    private fun updateTags(
        anime: Anime,
        mediaTags: List<AbstractAniListWrapper.Tag>,
        currentAnimeTags: List<AnimeTag>,
        shortName: String
    ): Boolean {
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
                animeTagService.saveAll(
                    listOf(
                        AnimeTag(
                            anime = anime,
                            tag = tag,
                            isAdult = mediaTag.isAdult,
                            isSpoiler = isSpoiler
                        )
                    )
                )
            } else if (existingAnimeTag.isAdult != mediaTag.isAdult || existingAnimeTag.isSpoiler != isSpoiler) {
                existingAnimeTag.isAdult = mediaTag.isAdult
                existingAnimeTag.isSpoiler = isSpoiler
                animeTagService.update(existingAnimeTag)
            }
        }

        logger.info("Tags updated for anime $shortName to ${filteredMediaTags.joinToString { it.name }}")
        return true
    }

    private fun updateExistingTagsMetadata(
        mediaTags: List<AbstractAniListWrapper.Tag>,
        currentAnimeTags: List<AnimeTag>
    ): Boolean {
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