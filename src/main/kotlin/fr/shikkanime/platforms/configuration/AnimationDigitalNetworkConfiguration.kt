package fr.shikkanime.platforms.configuration

import fr.shikkanime.platforms.configuration.AnimationDigitalNetworkConfiguration.AnimationDigitalNetworkSimulcast
import io.ktor.http.*

class AnimationDigitalNetworkConfiguration : PlatformConfiguration<AnimationDigitalNetworkSimulcast>() {
    data class AnimationDigitalNetworkSimulcast(
        var audioLocaleDelay: Int? = null,
    ) : PlatformSimulcast() {
        override fun of(parameters: Parameters) {
            super.of(parameters)
            parameters["audioLocaleDelay"]?.let { audioLocaleDelay = it.toIntOrNull() }
        }

        override fun toConfigurationFields() = super.toConfigurationFields().apply {
            add(
                ConfigurationField(
                    label = "Audio Locale Delay",
                    name = "audioLocaleDelay",
                    type = "number",
                    value = audioLocaleDelay,
                )
            )
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AnimationDigitalNetworkSimulcast) return false
            if (!super.equals(other)) return false

            if (audioLocaleDelay != other.audioLocaleDelay) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + (audioLocaleDelay?.hashCode() ?: 0)
            return result
        }
    }

    override fun newPlatformSimulcast() = AnimationDigitalNetworkSimulcast()
}