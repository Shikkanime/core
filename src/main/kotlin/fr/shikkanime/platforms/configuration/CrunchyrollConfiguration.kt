package fr.shikkanime.platforms.configuration

import io.ktor.http.*

data class CrunchyrollConfiguration(
    var simulcastCheckDelayInMinutes: Long = 60,
) : PlatformConfiguration<PlatformSimulcast>() {
    override fun newPlatformSimulcast() = PlatformSimulcast()

    override fun of(parameters: Parameters) {
        super.of(parameters)
        parameters["simulcastCheckDelayInMinutes"]?.let { simulcastCheckDelayInMinutes = it.toLong() }
    }

    override fun toConfigurationFields() = super.toConfigurationFields().apply {
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