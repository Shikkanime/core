package fr.shikkanime.jobs

import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.platforms.AnimationDigitalNetworkPlatform
import fr.shikkanime.utils.Constant
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime

class FetchEpisodesJob : AbstractJob() {
    private val platformClasses: List<Class<out AbstractPlatform>> = listOf(AnimationDigitalNetworkPlatform::class.java)

    override fun run() {
        val zonedDateTime = ZonedDateTime.now().withNano(0)

        platformClasses.forEach { platformClass ->
            val abstractPlatform = Constant.guice.getInstance(platformClass)
            println("Fetching episodes for ${abstractPlatform.platform.name}...")

            runBlocking {
                abstractPlatform.fetchEpisodes(zonedDateTime)
            }
        }
    }
}