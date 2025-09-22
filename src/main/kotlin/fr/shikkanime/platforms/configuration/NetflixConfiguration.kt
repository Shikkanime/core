package fr.shikkanime.platforms.configuration

import fr.shikkanime.utils.StringUtils
import io.ktor.http.*

class NetflixConfiguration : PlatformConfiguration<NetflixConfiguration.NetflixSimulcastDay>() {
    data class NetflixSimulcastDay(
        var audioLocales: MutableSet<String> = mutableSetOf("ja-JP"),
        var audioLocaleHasDelay: MutableSet<String> = mutableSetOf(),
    ) : ReleaseDayPlatformSimulcast() {
        override fun of(parameters: Parameters) {
            super.of(parameters)
            parameters["audioLocales"]?.let { audioLocales = it.split(StringUtils.COMMA_STRING).toMutableSet() }
            parameters["audioLocaleHasDelay"]?.let { audioLocaleHasDelay = it.split(StringUtils.COMMA_STRING).toMutableSet() }
        }

        override fun toConfigurationFields() = super.toConfigurationFields().apply {
            add(
                ConfigurationField(
                    label = "Audio Locales",
                    name = "audioLocales",
                    type = "text",
                    value = audioLocales.joinToString(StringUtils.COMMA_STRING),
                )
            )
            add(
                ConfigurationField(
                    label = "Audio Locale Delays",
                    caption = "Format: locale (e.g. fr-FR)",
                    name = "audioLocaleHasDelay",
                    type = "text",
                    value = audioLocaleHasDelay.joinToString(StringUtils.COMMA_STRING),
                )
            )
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NetflixSimulcastDay) return false
            if (!super.equals(other)) return false

            if (audioLocales != other.audioLocales) return false
            if (audioLocaleHasDelay != other.audioLocaleHasDelay) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + audioLocales.hashCode()
            result = 31 * result + audioLocaleHasDelay.hashCode()
            return result
        }
    }

    override fun newPlatformSimulcast() = NetflixSimulcastDay()
}