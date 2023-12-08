package fr.shikkanime.jobs

import fr.shikkanime.entities.Episode
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.utils.Constant
import jakarta.inject.Inject
import java.time.ZonedDateTime

class FetchEpisodesJob : AbstractJob() {
    @Inject
    private lateinit var episodeService: EpisodeService

    override fun run() {
        val zonedDateTime = ZonedDateTime.now().withNano(0)
        val episodes = mutableListOf<Episode>()

        Constant.abstractPlatforms.forEach { abstractPlatform ->
            println("Fetching episodes for ${abstractPlatform.getPlatform().name}...")

            try {
                episodes.addAll(abstractPlatform.fetchEpisodes(zonedDateTime))
            } catch (e: Exception) {
                println("Error while fetching episodes for ${abstractPlatform.getPlatform().name}: ${e.message}")
                e.printStackTrace()
            }
        }

        episodes.forEach { episodeService.saveOrUpdate(it) }
    }
}