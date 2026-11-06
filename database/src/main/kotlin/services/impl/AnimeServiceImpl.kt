package fr.shikkanime.database.services.impl

import fr.shikkanime.core.pagination.Page
import fr.shikkanime.core.pagination.PageRequest
import fr.shikkanime.database.repositories.AnimeLocalizationRepository
import fr.shikkanime.database.repositories.AnimePlatformRepository
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Platform
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [AnimeService::class])
internal class AnimeServiceImpl(
    private val animeRepository: AnimeRepository,
    private val localizationRepository: AnimeLocalizationRepository,
    private val platformRepository: AnimePlatformRepository,
    private val imageRepository: ImageRepository
) : AnimeService {

    override fun findAll(): List<AnimeWithChildren> =
        // The public API asks the whole catalog in one read
        animeRepository.findAllModels()

    override fun findPage(request: PageRequest, search: String?): Page<AnimeWithChildren> =
        // Filter and compose only the requested page, never the whole catalog
        animeRepository.findPage(request, search)

    override fun findAllByIds(ids: List<Uuid>): Map<Uuid, Anime> =
        // Resolve a whole page of display rows in one grouped read
        animeRepository.findModelsByIds(ids)

    override fun findAllByNormalizedTitles(titles: List<String>): Map<String, AnimeWithChildren> =
        // Resolve in one read the animes already stored under the given normalized titles
        animeRepository.findAllByNormalizedTitles(titles)

    override fun findByNormalizedTitle(normalizedTitle: String): AnimeWithChildren? =
        // Resolve the anime behind a normalized title
        animeRepository.findByNormalizedTitle(normalizedTitle)

    override fun findByPlatformAndPlatformId(platform: Platform, platformId: String): AnimeWithChildren? =
        // Resolve the aggregate owning a platform source link
        animeRepository.findByPlatformAndPlatformId(platform, platformId)

    override fun findById(id: Uuid): AnimeWithChildren? =
        // Load the aggregate with its localized texts, sources and artwork
        animeRepository.findModelById(id)

    override fun save(anime: AnimeWithChildren): AnimeWithChildren {
        // Persist the identity first so every child row can reference the anime
        val saved = animeRepository.save(anime.anime)

        return AnimeWithChildren(
            anime = saved,
            // Refresh known locales in place and append the new ones, without deleting anything
            localizations = anime.localizations.map {
                localizationRepository.save(it.copy(animeId = saved.id))
            },
            // Keep the platform source links in sync with the aggregate
            platforms = anime.platforms.map {
                platformRepository.save(it.copy(animeId = saved.id))
            },
            // The saved anime owns its artwork: the owner is written here, exactly once
            images = anime.images.map { imageRepository.save(it.copy(entityId = saved.id)) }
        )
    }
}
