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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NetflixSimulcastDay) return false
            if (!super.equals(other)) return false

            if (releaseDay != other.releaseDay) return false
            if (image != other.image) return false
            if (releaseTime != other.releaseTime) return false
            if (season != other.season) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + releaseDay
            result = 31 * result + image.hashCode()
            result = 31 * result + releaseTime.hashCode()
            result = 31 * result + season
            return result
        }
    }

    override fun newPlatformSimulcast() = NetflixSimulcastDay()
}