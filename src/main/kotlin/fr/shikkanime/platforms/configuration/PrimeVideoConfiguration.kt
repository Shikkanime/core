package fr.shikkanime.platforms.configuration

import fr.shikkanime.entities.enums.EpisodeType
import io.ktor.http.*

class PrimeVideoConfiguration : PlatformConfiguration<PrimeVideoConfiguration.PrimeVideoSimulcast>() {
    data class PrimeVideoSimulcast(
        override var releaseDay: Int = 1,
        override var image: String = "",
        var releaseTime: String = "",
        var episodeType: EpisodeType = EpisodeType.EPISODE,
        var audioLocales: MutableSet<String> = mutableSetOf("ja-JP"),
    ) : ReleaseDayPlatformSimulcast(releaseDay, image) {
        override fun of(parameters: Parameters) {
            super.of(parameters)
            parameters["releaseTime"]?.let { releaseTime = it }
            parameters["episodeType"]?.let { episodeType = EpisodeType.valueOf(it) }
            parameters["audioLocales"]?.let { audioLocales = it.split(",").toMutableSet() }
        }

        override fun toConfigurationFields() = super.toConfigurationFields().apply {
            add(
                ConfigurationField(
                    label = "Release time",
                    caption = "Format: HH:mm:ss (In UTC)",
                    name = "releaseTime",
                    type = "time",
                    value = releaseTime
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

    override fun newPlatformSimulcast() = PrimeVideoSimulcast()
}