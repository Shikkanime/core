package fr.shikkanime.platforms.configuration

import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.utils.StringUtils
import io.ktor.http.*

class PrimeVideoConfiguration : PlatformConfiguration<PrimeVideoConfiguration.PrimeVideoSimulcast>() {
    data class PrimeVideoSimulcast(
        var releaseTime: String = StringUtils.EMPTY_STRING,
        var episodeType: EpisodeType = EpisodeType.EPISODE,
    ) : ReleaseDayPlatformSimulcast() {
        override fun of(parameters: Parameters) {
            super.of(parameters)
            parameters["releaseTime"]?.let { releaseTime = it }
            parameters["episodeType"]?.let { episodeType = EpisodeType.valueOf(it) }
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
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PrimeVideoSimulcast) return false
            if (!super.equals(other)) return false

            if (releaseTime != other.releaseTime) return false
            if (episodeType != other.episodeType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + releaseTime.hashCode()
            result = 31 * result + episodeType.hashCode()
            return result
        }
    }

    override fun newPlatformSimulcast() = PrimeVideoSimulcast()
}