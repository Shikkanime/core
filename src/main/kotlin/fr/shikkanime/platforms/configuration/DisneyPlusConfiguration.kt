package fr.shikkanime.platforms.configuration

import io.ktor.http.*

data class DisneyPlusConfiguration(
    var authorization: String = "",
    var refreshToken: String = "",
) : PlatformConfiguration<PlatformSimulcast>() {
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