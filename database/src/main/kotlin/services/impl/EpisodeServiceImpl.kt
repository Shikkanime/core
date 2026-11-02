package fr.shikkanime.database.services.impl

import fr.shikkanime.database.repositories.EpisodeLocalizationRepository
import fr.shikkanime.database.repositories.EpisodeReleaseRepository
import fr.shikkanime.database.repositories.EpisodeRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.EpisodeWithChildren
import fr.shikkanime.models.Platform
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [EpisodeService::class])
internal class EpisodeServiceImpl(
    private val episodeRepository: EpisodeRepository,
    private val localizationRepository: EpisodeLocalizationRepository,
    private val releaseRepository: EpisodeReleaseRepository,
    private val imageRepository: ImageRepository
) : EpisodeService {

    override fun findAllByAnimeId(animeId: Uuid): List<EpisodeWithChildren> =
        // Load every episode of the anime with its texts, releases and artwork
        episodeRepository.findAllByAnimeId(animeId)

    override fun findById(id: Uuid): EpisodeWithChildren? =
        // Load the aggregate with its localized texts, releases and artwork
        episodeRepository.findModelById(id)

    override fun findByAnimeIdAndSeasonAndNumberAndEpisodeType(
        animeId: Uuid,
        season: Int,
        number: Int,
        episodeType: EpisodeType
    ): EpisodeWithChildren? =
        // Resolve the episode carrying an identity under its anime
        episodeRepository.findByAnimeIdAndSeasonAndNumberAndEpisodeType(
            animeId,
            season,
            number,
            episodeType
        )

    override fun findByPlatformAndPlatformId(
        platform: Platform,
        platformId: String
    ): EpisodeWithChildren? =
        // Resolve the aggregate owning a platform release
        episodeRepository.findByPlatformAndPlatformId(platform, platformId)

    override fun save(episode: EpisodeWithChildren): EpisodeWithChildren {
        // The caller resolved the identity: a null identifier inserts a new episode, never duplicates
        val saved = episodeRepository.save(episode)

        return EpisodeWithChildren(
            episode = saved,
            // The saved episode owns its artwork: the owner is written here, exactly once
            images = episode.images.map { imageRepository.save(it.copy(entityId = saved.id)) },
            // Refresh known locales in place and append the new ones, without deleting anything
            localizations = episode.localizations.map {
                localizationRepository.save(it.copy(episodeId = saved.id))
            },
            // Keep the releases in sync with the aggregate
            releases = episode.releases.map {
                releaseRepository.save(it.copy(episodeId = saved.id))
            }
        )
    }
}
