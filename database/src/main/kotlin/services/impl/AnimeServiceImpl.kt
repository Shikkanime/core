package fr.shikkanime.database.services.impl

import fr.shikkanime.database.repositories.AnimeLocalizationRepository
import fr.shikkanime.database.repositories.AnimePlatformRepository
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.models.Anime
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

    override fun findAll(): List<Anime> =
        // Load every aggregate with its texts, sources and artwork
        animeRepository.findAllModels()

    override fun findByPlatformAndPlatformId(platform: Platform, platformId: String): Anime? =
        // Resolve the aggregate owning a platform source link
        animeRepository.findByPlatformAndPlatformId(platform, platformId)

    override fun findById(id: Uuid): Anime? =
        // Load the aggregate with its localized texts, sources and artwork
        animeRepository.findModelById(id)

    override fun save(anime: Anime): Anime {
        // Persist the identity first so every child row can reference the anime
        val saved = animeRepository.save(anime)

        return saved.copy(
            // Refresh known locales in place and append the new ones, without deleting anything
            localizations = anime.localizations.map { localizationRepository.save(anime, it) },
            // Keep the platform source links in sync with the aggregate
            platforms = anime.platforms.map { platformRepository.save(anime, it) },
            // Store images under the anime identifier, whatever owner the given images carry
            images = anime.images.map { imageRepository.save(it.copy(entityId = saved.id)) }
        )
    }
}
