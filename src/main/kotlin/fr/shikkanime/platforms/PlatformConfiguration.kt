package fr.shikkanime.platforms

import fr.shikkanime.entities.enums.CountryCode
import io.ktor.http.*

data class ConfigurationField(
    val label: String,
    val name: String,
    val type: String,
    val value: Any,
)

open class PlatformConfiguration(
    var availableCountries: MutableSet<CountryCode> = mutableSetOf(),
    var apiCheckDelayInMinutes: Long = 0,
    var simulcasts: MutableSet<String> = mutableSetOf(),
) {
    open fun of(parameters: Parameters) {
        parameters["availableCountries"]?.let {
            if (it.isBlank()) {
                availableCountries = mutableSetOf()
                return@let
            }

            availableCountries = CountryCode.from(it.split(",")) as MutableSet<CountryCode>
        }

        parameters["apiCheckDelayInMinutes"]?.let {
            apiCheckDelayInMinutes = it.toLong()
        }

        (parameters.getAll("simulcasts") ?: emptyList()).let {
            val toMutableSet = it.toMutableSet()
            toMutableSet.removeIf { simulcast -> simulcast.isBlank() }
            simulcasts = toMutableSet
        }
    }

    open fun toConfigurationFields(): MutableSet<ConfigurationField> {
        return mutableSetOf(
            ConfigurationField(
                label = "Available countries",
                name = "availableCountries",
                type = "text",
                value = availableCountries.joinToString(",")
            ),
            ConfigurationField(
                label = "API check delay in minutes",
                name = "apiCheckDelayInMinutes",
                type = "number",
                value = apiCheckDelayInMinutes.toString()
            ),
            ConfigurationField(
                label = "Simulcasts",
                name = "simulcasts",
                type = "list",
                value = simulcasts
            )
        )
    }
}