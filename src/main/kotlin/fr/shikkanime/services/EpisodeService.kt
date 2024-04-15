package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Episode
import fr.shikkanime.repositories.EpisodeRepository

@Deprecated("Use EpisodeMappingService instead")
class EpisodeService : AbstractService<Episode, EpisodeRepository>() {
    @Inject
    @Deprecated("Use EpisodeMappingRepository instead")
    private lateinit var episodeRepository: EpisodeRepository

    override fun getRepository() = episodeRepository
}