package fr.shikkanime.platforms.configuration

import fr.shikkanime.entities.enums.EpisodeType
import io.ktor.http.*

class NetflixConfiguration : PlatformConfiguration<NetflixConfiguration.NetflixSimulcastDay>() {
    data class NetflixSimulcastDay(
        override var releaseDay: Int = 1,
        override var image: String = "",
        override var releaseTime: String = "",
        var seasonName: String = "",
        var season: Int = 1,
        var episodeType: EpisodeType = EpisodeType.EPISODE,
    ) : ReleaseDayPlatformSimulcast(releaseDay, image, releaseTime) {
        override fun of(parameters: Parameters) {
            super.of(parameters)
            parameters["seasonName"]?.let { seasonName = it }
            parameters["season"]?.let { season = it.toInt() }
            parameters["episodeType"]?.let { episodeType = EpisodeType.valueOf(it) }
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
        }
    }

    override fun newPlatformSimulcast() = NetflixSimulcastDay()
}