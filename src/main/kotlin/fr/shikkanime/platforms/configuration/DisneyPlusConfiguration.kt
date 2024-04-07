package fr.shikkanime.platforms.configuration

import io.ktor.http.*

data class DisneyPlusConfiguration(
    var authorization: String = "",
    var refreshToken: String = "",
) : PlatformConfiguration<DisneyPlusConfiguration.DisneyPlusSimulcast>() {
    data class DisneyPlusSimulcast(
        var releaseDay: Int = 1,
        var releaseTime: String = "",
    ) : PlatformSimulcast() {
        override fun of(parameters: Parameters) {
            super.of(parameters)
            parameters["releaseDay"]?.let { releaseDay = it.toInt() }
            parameters["releaseTime"]?.let { releaseTime = it }
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
            if (other !is DisneyPlusSimulcast) return false
            if (!super.equals(other)) return false

            if (releaseDay != other.releaseDay) return false
            if (releaseTime != other.releaseTime) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + releaseDay
            result = 31 * result + releaseTime.hashCode()
            return result
        }
    }

    override fun newPlatformSimulcast() = DisneyPlusSimulcast()

    override fun of(parameters: Parameters) {
        super.of(parameters)
        parameters["authorization"]?.let { authorization = it }
        parameters["refreshToken"]?.let { refreshToken = it }
    }

    override fun toConfigurationFields() = super.toConfigurationFields().apply {
        add(
            ConfigurationField(
                label = "Authorization",
                name = "authorization",
                type = "text",
                value = authorization
            )
        )
        add(
            ConfigurationField(
                label = "Refresh token",
                name = "refreshToken",
                type = "text",
                value = refreshToken
            )
        )
    }
}