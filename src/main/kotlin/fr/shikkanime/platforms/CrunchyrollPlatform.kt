package fr.shikkanime.platforms

import fr.shikkanime.entities.Platform
import java.time.ZonedDateTime

class CrunchyrollPlatform : AbstractPlatform<CrunchyrollPlatform.CrunchyrollConfiguration>() {
    data class CrunchyrollConfiguration(
        @Configuration(
            label = "Simulcast check delay in minutes",
            type = "number",
        )
        @ConfigurationConverter(converter = LongFieldConverter::class)
        var simulcastCheckDelayInMinutes: Long = 60,
    ) : PlatformConfiguration()

    override fun getConfigurationClass() = CrunchyrollConfiguration::class.java

    override fun getPlatform(): Platform {
        val name = "Crunchyroll"

        return platformService.findByName(name) ?: Platform(
            name = name,
            url = "https://www.crunchyroll.com/",
            image = "crunchyroll.png",
        )
    }

    override suspend fun fetchApiContent(zonedDateTime: ZonedDateTime): Api {
        TODO("Not yet implemented")
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime): List<String> {
        TODO("Not yet implemented")
    }

    override fun reset() {
        TODO("Not yet implemented")
    }
}