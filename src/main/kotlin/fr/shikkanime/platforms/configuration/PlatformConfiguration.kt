package fr.shikkanime.platforms.configuration

import fr.shikkanime.entities.enums.CountryCode
import io.ktor.http.*
import java.lang.reflect.ParameterizedType
import java.util.*

data class ConfigurationField(
    val label: String,
    val caption: String? = null,
    val name: String,
    val type: String,
    val value: Any? = null
)

open class PlatformSimulcast(
    var uuid: UUID? = null,
    var name: String = "",
) {
    open fun of(parameters: Parameters) {
        parameters["name"]?.let { name = it }
    }

    open fun toConfigurationFields(): MutableSet<ConfigurationField> {
        return mutableSetOf(
            ConfigurationField(
                label = "Name",
                name = "name",
                type = "text",
                value = name
            ),
        )
    }
}

open class PlatformConfiguration<S : PlatformSimulcast>(
    var availableCountries: MutableSet<CountryCode> = mutableSetOf(),
    var apiCheckDelayInMinutes: Long = 0,
    val simulcasts: MutableSet<S> = mutableSetOf(),
    val blacklistedSimulcasts: MutableSet<String> = mutableSetOf(),
) {
    fun newPlatformSimulcast(): S {
        @Suppress("UNCHECKED_CAST")
        return ((javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<S>).getConstructor()
            .newInstance()
    }

    fun addPlatformSimulcast(simulcast: PlatformSimulcast) {
        @Suppress("UNCHECKED_CAST")
        simulcasts.add(simulcast as S)
    }

    open fun of(parameters: Parameters) {
        parameters["availableCountries"]?.let {
            availableCountries = if (it.isNotBlank())
                CountryCode.from(it.split(",")) as MutableSet<CountryCode>
            else
                mutableSetOf()
        }

        parameters["apiCheckDelayInMinutes"]?.let { apiCheckDelayInMinutes = it.toLong() }

        parameters["blacklistedSimulcasts"]?.let {
            blacklistedSimulcasts.clear()
            blacklistedSimulcasts.addAll(it.split("||"))
        }
    }

    open fun toConfigurationFields() = mutableSetOf(
        ConfigurationField(
            label = "Available countries",
            name = "availableCountries",
            type = "text",
            value = availableCountries.joinToString(",")
        ),
        ConfigurationField(
            label = "API check delay",
            caption = "In minutes",
            name = "apiCheckDelayInMinutes",
            type = "number",
            value = apiCheckDelayInMinutes.toString()
        ),
        ConfigurationField(
            label = "Blacklisted simulcasts",
            caption = "Separate with ||",
            name = "blacklistedSimulcasts",
            type = "textarea",
            value = blacklistedSimulcasts.joinToString("||")
        ),
    )
}