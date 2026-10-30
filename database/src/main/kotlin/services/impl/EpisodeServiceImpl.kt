package fr.shikkanime.database.services.impl

import fr.shikkanime.database.repositories.EpisodeLocalizationRepository
import fr.shikkanime.database.repositories.EpisodePlatformRepository
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
    private val platformRepository: EpisodePlatformRepository,
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

    /**
     * Resolves the stored episode an incoming aggregate belongs to.
     *
     * The platform release is the strongest identity: a video renumbered on its platform keeps
     * pointing at the same episode. The identity `(anime, season, type, number)` is the fallback of
     * a release seen for the first time.
     *
     * @param episode aggregate about to be persisted.
     * @return the identifier of the stored episode, or `null` when it is a new one.
     * @see [findEpisodeOfPlatform]
     */
    private fun findStoredEpisodeId(episode: EpisodeWithChildren): Uuid? =
        findEpisodeOfPlatform(episode)?.episode?.id
            ?: episodeRepository.findByAnimeIdAndSeasonAndNumberAndEpisodeType(
                requireNotNull(episode.episode.animeId),
                episode.episode.season,
                episode.episode.number,
                episode.episode.episodeType
            )?.episode?.id

    /**
     * Finds the stored episode carrying one of the platform releases of an aggregate.
     *
     * A release stored under another anime is ignored: a renamed anime is a new identity, so its
     * episodes are attached to it rather than reusing the row of the previous one.
     *
     * @param episode aggregate about to be persisted.
     * @return the stored episode carrying one of these releases, or `null` when none does.
     * @see [EpisodeWithChildren]
     */
    private fun findEpisodeOfPlatform(episode: EpisodeWithChildren): EpisodeWithChildren? =
        episode.platforms.firstNotNullOfOrNull { platform ->
            episodeRepository.findByPlatformAndPlatformId(platform.platform, platform.platformId)
                ?.takeIf { it.episode.animeId == episode.episode.animeId }
        }

    override fun save(episode: EpisodeWithChildren): EpisodeWithChildren {
        // Re-apply the stored identity so a re-harvest updates the episode instead of duplicating it
        val identity = episode.episode.id ?: findStoredEpisodeId(episode)
        val saved = episodeRepository.save(episode.copy(episode = episode.episode.copy(id = identity)))

        return EpisodeWithChildren(
            episode = saved,
            // The saved episode owns its artwork: the owner is written here, exactly once
            images = episode.images.map { imageRepository.save(it.copy(entityId = saved.id)) },
            // Refresh known locales in place and append the new ones, without deleting anything
            localizations = episode.localizations.map {
                localizationRepository.save(it.copy(episodeId = saved.id))
            },
            // Keep the platform releases in sync with the aggregate
            platforms = episode.platforms.map {
                platformRepository.save(it.copy(episodeId = saved.id))
            }
        )
    }
}
