package fr.shikkanime.platforms.configuration

import io.ktor.http.*

class NetflixConfiguration : PlatformConfiguration<NetflixConfiguration.NetflixSimulcastDay>() {
    data class NetflixSimulcastDay(
        override var releaseDay: Int = 1,
        override var image: String = "",
        override var releaseTime: String = "",
        var season: Int = 1,
    ) : ReleaseDayPlatformSimulcast(releaseDay, image, releaseTime) {
        override fun of(parameters: Parameters) {
            super.of(parameters)
            parameters["season"]?.let { season = it.toInt() }
        }

        override fun toConfigurationFields() = super.toConfigurationFields().apply {
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

    override fun newPlatformSimulcast() = NetflixSimulcastDay()
}