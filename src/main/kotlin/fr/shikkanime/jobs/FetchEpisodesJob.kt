package fr.shikkanime.jobs

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.Episode
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.utils.*
import jakarta.inject.Inject
import java.time.ZonedDateTime
import java.util.logging.Level


class FetchEpisodesJob : AbstractJob() {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var isInitialized = false
    private var isRunning = false
    private val set = mutableSetOf<String>()

    @Inject
    private lateinit var episodeService: EpisodeService

    override fun run() {
        if (isRunning) {
            logger.warning("Job is already running")
            return
        }

        isRunning = true

        if (!isInitialized) {
            val hashes = episodeService.findAllHashes()

            set.addAll(hashes)
            Constant.abstractPlatforms.forEach { it.hashCache.addAll(hashes) }
            isInitialized = true
        }

        val zonedDateTime = ZonedDateTime.now().withSecond(0).withNano(0).withUTC()
        val episodes = mutableListOf<Episode>()

        Constant.abstractPlatforms.forEach { abstractPlatform ->
            logger.info("Fetching episodes for ${abstractPlatform.getPlatform().name}...")

            try {
                episodes.addAll(abstractPlatform.fetchEpisodes(zonedDateTime))
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error while fetching episodes for ${abstractPlatform.getPlatform().name}", e)
            }
        }

        episodes
            .filter { (zonedDateTime.isEqualOrAfter(it.releaseDateTime)) && !set.contains(it.hash) }
            .forEach {
                try {
                    val savedEpisode = episodeService.save(it)
                    savedEpisode.hash?.let { hash -> set.add(hash) }
                    val dto = AbstractConverter.convert(savedEpisode, EpisodeDto::class.java)
                    sendToSocialNetworks(dto)
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error while saving episode ${it.hash} (${it.anime?.name})", e)
                }
            }

        isRunning = false
    }

    private fun sendToSocialNetworks(dto: EpisodeDto) {
        Constant.abstractSocialNetworks.forEach { socialNetwork ->
            try {
                socialNetwork.sendEpisodeRelease(dto)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error while sending episode release for ${socialNetwork.javaClass.simpleName.replace("SocialNetwork", "")}", e)
            }
        }
    }
}