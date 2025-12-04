package fr.shikkanime.platforms.configuration

import fr.shikkanime.utils.StringUtils
import io.ktor.http.*
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PrimeVideoConfiguration : PlatformConfiguration<PrimeVideoConfiguration.PrimeVideoSimulcast>() {
    data class PrimeVideoSimulcast(
        var image: String = StringUtils.EMPTY_STRING,
        var releaseTime: String = StringUtils.EMPTY_STRING
    ) : ReleaseDayPlatformSimulcast() {
        override fun of(parameters: Parameters) {
            super.of(parameters)
            parameters["image"]?.let { image = it }
            parameters["releaseTime"]?.let { releaseTime = it }
        }

        override fun toConfigurationFields() = super.toConfigurationFields().apply {
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
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PrimeVideoSimulcast) return false
            if (!super.equals(other)) return false

            if (image != other.image) return false
            if (releaseTime != other.releaseTime) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + image.hashCode()
            result = 31 * result + releaseTime.hashCode()
            return result
        }

        override fun canBeFetch(zonedDateTime: ZonedDateTime): Boolean {
            if (!super.canBeFetch(zonedDateTime)) return false
            if (this.releaseTime.isBlank()) return true

            val releaseLocalTime = LocalTime.parse(this.releaseTime)
            val isAfterInOriginalZone = zonedDateTime.toLocalTime() >= releaseLocalTime
            val isAfterInSystemZone = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalTime() >= releaseLocalTime

            return isAfterInOriginalZone || isAfterInSystemZone
        }
    }

    override fun newPlatformSimulcast() = PrimeVideoSimulcast()
}