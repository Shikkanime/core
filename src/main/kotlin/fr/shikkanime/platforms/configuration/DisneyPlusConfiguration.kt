package fr.shikkanime.platforms.configuration

import io.ktor.http.*

class DisneyPlusConfiguration : PlatformConfiguration<DisneyPlusConfiguration.DisneyPlusSimulcast>() {
    data class DisneyPlusSimulcast(
        var releaseDay: Int = 1,
    ) : PlatformSimulcast() {
        override fun of(parameters: Parameters) {
            super.of(parameters)
            parameters["releaseDay"]?.let { releaseDay = it.toInt() }
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
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DisneyPlusSimulcast) return false
            if (!super.equals(other)) return false

            if (releaseDay != other.releaseDay) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + releaseDay
            return result
        }
    }

    override fun newPlatformSimulcast() = DisneyPlusSimulcast()
}