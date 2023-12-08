package fr.shikkanime.platforms

import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.Platform
import io.ktor.http.*
import java.time.ZonedDateTime

class CrunchyrollPlatform : AbstractPlatform<CrunchyrollPlatform.CrunchyrollConfiguration>() {
    data class CrunchyrollConfiguration(
        var simulcastCheckDelayInMinutes: Long = 60,
    ) : PlatformConfiguration() {
        override fun of(parameters: Parameters) {
            super.of(parameters)
            parameters["simulcastCheckDelayInMinutes"]?.let { simulcastCheckDelayInMinutes = it.toLong() }
        }

        override fun toConfigurationFields(): MutableSet<ConfigurationField> {
            return super.toConfigurationFields().apply {
                add(
                    ConfigurationField(
                        label = "Simulcast check delay in minutes",
                        name = "simulcastCheckDelayInMinutes",
                        type = "number",
                        value = simulcastCheckDelayInMinutes.toString()
                    )
                )
            }
        }
    }

    override fun getConfigurationClass() = CrunchyrollConfiguration::class.java

    override fun getPlatform(): Platform {
        return Platform(
            name = "Crunchyroll",
            url = "https://www.crunchyroll.com/",
            image = "crunchyroll.png",
        )
    }

    override suspend fun fetchApiContent(zonedDateTime: ZonedDateTime): Api {
        TODO("Not yet implemented")
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime): List<Episode> {
        return emptyList()
    }

    override fun reset() {
        TODO("Not yet implemented")
    }
}