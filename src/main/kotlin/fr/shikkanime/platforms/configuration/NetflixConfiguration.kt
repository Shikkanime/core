package fr.shikkanime.platforms.configuration

import fr.shikkanime.entities.enums.EpisodeType
import io.ktor.http.*

class NetflixConfiguration : PlatformConfiguration<NetflixConfiguration.NetflixSimulcastDay>() {
    data class NetflixSimulcastDay(
        override var releaseDay: Int = 1,
        override var image: String = "",
        var seasonName: String = "",
        var season: Int = 1,
        var episodeType: EpisodeType = EpisodeType.EPISODE,
        var audioLocales: MutableSet<String> = mutableSetOf("ja-JP"),
    ) : ReleaseDayPlatformSimulcast(releaseDay, image) {
        override fun of(parameters: Parameters) {
            super.of(parameters)
            parameters["seasonName"]?.let { seasonName = it }
            parameters["season"]?.let { season = it.toInt() }
            parameters["episodeType"]?.let { episodeType = EpisodeType.valueOf(it) }
            parameters["audioLocales"]?.let { audioLocales = it.split(",").toMutableSet() }
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
        }
    }

    override fun newPlatformSimulcast() = NetflixSimulcastDay()
}