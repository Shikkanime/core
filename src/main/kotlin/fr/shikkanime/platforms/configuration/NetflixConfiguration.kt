package fr.shikkanime.platforms.configuration

import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.utils.StringUtils
import io.ktor.http.*

class NetflixConfiguration : PlatformConfiguration<NetflixConfiguration.NetflixSimulcastDay>() {
    data class NetflixSimulcastDay(
        var episodeType: EpisodeType = EpisodeType.EPISODE,
        var audioLocales: MutableSet<String> = mutableSetOf("ja-JP"),
        var audioLocaleDelays: MutableMap<String, Long> = mutableMapOf(),
    ) : ReleaseDayPlatformSimulcast() {
        override fun of(parameters: Parameters) {
            super.of(parameters)
            parameters["episodeType"]?.let { episodeType = EpisodeType.valueOf(it) }
            parameters["audioLocales"]?.let { audioLocales = it.split(StringUtils.COMMA_STRING).toMutableSet() }
            parameters["audioLocaleDelays"]?.let {
                audioLocaleDelays.clear()
                it.split(StringUtils.COMMA_STRING).forEach { delay ->
                    val parts = delay.split(":")
                    if (parts.size == 2) {
                        audioLocaleDelays[parts[0]] = parts[1].toLongOrNull() ?: 0
                    }
                }
            }
        }

        override fun toConfigurationFields() = super.toConfigurationFields().apply {
            add(
                ConfigurationField(
                    label = "Episode Type",
                    name = "episodeType",
                    type = "text",
                    value = episodeType.name,
                )
            )
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
                    caption = "Format: locale:weeks_delay,locale:weeks_delay (e.g. fr-FR:3)",
                    name = "audioLocaleDelays",
                    type = "text",
                    value = audioLocaleDelays.entries.joinToString(StringUtils.COMMA_STRING) { "${it.key}:${it.value}" },
                )
            )
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NetflixSimulcastDay) return false
            if (!super.equals(other)) return false

            if (episodeType != other.episodeType) return false
            if (audioLocales != other.audioLocales) return false
            if (audioLocaleDelays != other.audioLocaleDelays) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + episodeType.hashCode()
            result = 31 * result + audioLocales.hashCode()
            result = 31 * result + audioLocaleDelays.hashCode()
            return result
        }
    }

    override fun newPlatformSimulcast() = NetflixSimulcastDay()
}