package fr.shikkanime.jobs

import fr.shikkanime.entities.Episode
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.utils.Constant
import jakarta.inject.Inject
import java.time.ZonedDateTime

class FetchEpisodesJob : AbstractJob() {
    private var isInitialized = false
    private var isRunning = false
    private val set = mutableSetOf<String>()

    @Inject
    private lateinit var episodeService: EpisodeService

    override fun run() {
        if (isRunning) {
            println("Job is already running")
            return
        }

        isRunning = true

        if (!isInitialized) {
            val hashes = episodeService.findAllHashes()

            set.addAll(hashes)
            Constant.abstractPlatforms.forEach { it.hashCache.addAll(hashes) }
            isInitialized = true
        }

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

        episodes
            .filter {
                (zonedDateTime.isEqual(it.releaseDateTime) || zonedDateTime.isAfter(it.releaseDateTime)) && !set.contains(
                    it.hash
                )
            }
            .forEach {
                val savedEpisode = episodeService.save(it)
                savedEpisode.hash?.let { hash -> set.add(hash) }
            }

        isRunning = false
    }
}