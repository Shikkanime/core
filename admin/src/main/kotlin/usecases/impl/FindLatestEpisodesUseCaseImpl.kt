package fr.shikkanime.admin.usecases.impl

import fr.shikkanime.admin.dtos.episode.AdminLatestEpisodeDto
import fr.shikkanime.admin.dtos.episode.EpisodeWithAnimeTitle
import fr.shikkanime.admin.dtos.episode.toAdminLatestDto
import fr.shikkanime.admin.usecases.FindLatestEpisodesUseCase
import fr.shikkanime.core.pagination.PageRequest
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.ktor.pagination.PageDto
import fr.shikkanime.ktor.pagination.toDto
import fr.shikkanime.models.EpisodeWithChildren
import org.koin.core.annotation.Single

@Single
class FindLatestEpisodesUseCaseImpl(
    private val episodeService: EpisodeService,
    private val animeService: AnimeService
) : FindLatestEpisodesUseCase {
    override fun findLatest(request: PageRequest): PageDto<AdminLatestEpisodeDto> {
        val episodes = episodeService.findPageByReleaseDate(request)

        // Resolve the animes of the page in one read, keyed by identifier for the title lookup
        val animes = animeService.findAllByIds(
            episodes.data.mapNotNull(EpisodeWithChildren::animeId).distinct()
        )

        return episodes.toDto { episode ->
            EpisodeWithAnimeTitle(
                episode,
                animes[episode.animeId]?.normalizedTitle.orEmpty()
            ).toAdminLatestDto()
        }
    }
}
