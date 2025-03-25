package fr.shikkanime.platforms.configuration

import fr.shikkanime.entities.enums.EpisodeType
import io.ktor.http.*

class NetflixConfiguration : PlatformConfiguration<NetflixConfiguration.NetflixSimulcastDay>() {
    data class NetflixSimulcastDay(
        var seasonName: String = "",
        var season: Int = 1,
        var episodeType: EpisodeType = EpisodeType.EPISODE,
        var audioLocales: MutableSet<String> = mutableSetOf("ja-JP"),
        var audioLocaleDelays: MutableMap<String, Int> = mutableMapOf(),
    ) : ReleaseDayPlatformSimulcast() {
        override fun of(parameters: Parameters) {
            super.of(parameters)
            parameters["seasonName"]?.let { seasonName = it }
            parameters["season"]?.let { season = it.toInt() }
            parameters["episodeType"]?.let { episodeType = EpisodeType.valueOf(it) }
            parameters["audioLocales"]?.let { audioLocales = it.split(",").toMutableSet() }
            parameters["audioLocaleDelays"]?.let {
                audioLocaleDelays.clear()
                it.split(",").forEach { delay ->
                    val parts = delay.split(":")
                    if (parts.size == 2) {
                        audioLocaleDelays[parts[0]] = parts[1].toIntOrNull() ?: 0
                    }
                }
            }
        }

        override fun toConfigurationFields() = super.toConfigurationFields().apply {
            add(
                ConfigurationField(
                    label = "Season Name",
                    name = "seasonName",
                    type = "text",
                    value = seasonName,
                ),
            )
            add(
                ConfigurationField(
                    label = "Season",
                    name = "season",
                    type = "number",
                    value = season,
                ),
            )
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
                    value = audioLocales.joinToString(","),
                )
            )
            add(
                ConfigurationField(
                    label = "Audio Locale Delays",
                    caption = "Format: locale:weeks_delay,locale:weeks_delay (e.g. fr-FR:3)",
                    name = "audioLocaleDelays",
                    type = "text",
                    value = audioLocaleDelays.entries.joinToString(",") { "${it.key}:${it.value}" },
                )
            )
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NetflixSimulcastDay) return false
            if (!super.equals(other)) return false

            if (season != other.season) return false
            if (seasonName != other.seasonName) return false
            if (episodeType != other.episodeType) return false
            if (audioLocales != other.audioLocales) return false
            if (audioLocaleDelays != other.audioLocaleDelays) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + season
            result = 31 * result + seasonName.hashCode()
            result = 31 * result + episodeType.hashCode()
            result = 31 * result + audioLocales.hashCode()
            result = 31 * result + audioLocaleDelays.hashCode()
            return result
        }
    }

    override fun newPlatformSimulcast() = NetflixSimulcastDay()
}