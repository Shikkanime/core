package fr.shikkanime.platforms.configuration

import fr.shikkanime.entities.enums.CountryCode
import io.ktor.http.*
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformSimulcast) return false

        if (uuid != other.uuid) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        return result
    }
}

abstract class PlatformConfiguration<S : PlatformSimulcast>(
    var availableCountries: Collection<CountryCode> = emptySet(),
    var apiCheckDelayInMinutes: Long = 0,
    val simulcasts: MutableSet<S> = mutableSetOf(),
    val blacklistedSimulcasts: MutableSet<String> = mutableSetOf(),
) {
    abstract fun newPlatformSimulcast(): S

    @Suppress("UNCHECKED_CAST")
    fun addPlatformSimulcast(simulcast: PlatformSimulcast) = simulcasts.add(simulcast as S)

    open fun of(parameters: Parameters) {
        parameters["availableCountries"]?.let {
            availableCountries = if (it.isNotBlank()) CountryCode.from(it.split(",")) else emptySet()
        }
        parameters["apiCheckDelayInMinutes"]?.let { apiCheckDelayInMinutes = it.toLong() }
        parameters["blacklistedSimulcasts"]?.let {
            blacklistedSimulcasts.clear()
            blacklistedSimulcasts.addAll(it.split("||"))
        }
    }

    open fun toConfigurationFields() = mutableSetOf(
        ConfigurationField(
            "Available countries",
            name = "availableCountries",
            type = "text",
            value = availableCountries.joinToString(",")
        ),
        ConfigurationField(
            "API check delay",
            "In minutes",
            "apiCheckDelayInMinutes",
            "number",
            apiCheckDelayInMinutes.toString()
        ),
        ConfigurationField(
            "Blacklisted simulcasts",
            "Separate with ||",
            "blacklistedSimulcasts",
            "textarea",
            blacklistedSimulcasts.joinToString("||")
        )
    )
}