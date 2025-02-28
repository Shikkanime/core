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
    }

    override fun newPlatformSimulcast() = DisneyPlusSimulcast()
}