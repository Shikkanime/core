package fr.shikkanime.jobs

import fr.shikkanime.utils.Constant
import java.time.ZonedDateTime

class FetchEpisodesJob : AbstractJob() {
    override fun run() {
        val zonedDateTime = ZonedDateTime.now().withNano(0)

        Constant.abstractPlatforms.forEach { abstractPlatform ->
            println("Fetching episodes for ${abstractPlatform.getPlatform().name}...")

            try {
                abstractPlatform.fetchEpisodes(zonedDateTime)
            } catch (e: Exception) {
                println("Error while fetching episodes for ${abstractPlatform.getPlatform().name}: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}