package fr.shikkanime.platforms.configuration

import io.ktor.http.*

class NetflixConfiguration : PlatformConfiguration<NetflixConfiguration.NetflixSimulcast>() {
    data class NetflixSimulcast(
        var releaseDay: Int = 1,
        var image: String = "",
        var releaseTime: String = "",
        var season: Int = 1,
    ) : PlatformSimulcast() {
        override fun of(parameters: Parameters) {
            super.of(parameters)
            parameters["releaseDay"]?.let { releaseDay = it.toInt() }
            parameters["image"]?.let { image = it }
            parameters["releaseTime"]?.let { releaseTime = it }
            parameters["season"]?.let { season = it.toInt() }
        }

        override fun toConfigurationFields() = super.toConfigurationFields().apply {
            add(
                ConfigurationField(
                    label = "Release day",
                    caption = "1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday, 6 = Saturday, 7 = Sunday",
                    name = "releaseDay",
                    type = "number",
                    value = releaseDay
                ),
            )
            add(
                ConfigurationField(
                    label = "Image",
                    name = "image",
                    type = "text",
                    value = image
                ),
            )
            add(
                ConfigurationField(
                    label = "Release time",
                    caption = "Format: HH:mm:ss (In UTC)",
                    name = "releaseTime",
                    type = "time",
                    value = releaseTime
                ),
            )
            add(
                ConfigurationField(
                    label = "Season",
                    name = "season",
                    type = "number",
                    value = season
                ),
            )
        }
    }
}