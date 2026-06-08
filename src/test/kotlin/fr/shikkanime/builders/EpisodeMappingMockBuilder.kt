package fr.shikkanime.builders

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.EpisodeType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.time.ZonedDateTime
import java.util.*

class EpisodeMappingMockBuilder {
    var uuid: UUID? = null
    var anime: Anime? = null
    var releaseDateTime: ZonedDateTime? = null
    var lastReleaseDateTime: ZonedDateTime? = null
    var lastUpdateDateTime: ZonedDateTime? = null
    var episodeType: EpisodeType? = null
    var season: Int? = null
    var number: Int? = null
    var duration: Long? = null
    var title: String? = null
    var description: String? = null
    var simulcast: Simulcast? = null

    fun build(): EpisodeMapping {
        val mockK = mockk<EpisodeMapping>()

        every { mockK.uuid } returns uuid
        every { mockK.anime } returns anime
        every { mockK.releaseDateTime } returns (releaseDateTime ?: ZonedDateTime.now())
        every { mockK.lastReleaseDateTime } returns (lastReleaseDateTime ?: mockK.releaseDateTime)
        every { mockK.lastUpdateDateTime } returns (lastUpdateDateTime ?: mockK.releaseDateTime)
        every { mockK.lastUpdateDateTime = any() } just Runs
        every { mockK.episodeType } returns episodeType
        every { mockK.season } returns season
        every { mockK.number } returns number
        every { mockK.duration } returns (duration ?: -1)
        every { mockK.title } returns title
        every { mockK.description } returns description
        every { mockK.simulcast } returns simulcast

        return mockK
    }
}