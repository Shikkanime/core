package fr.shikkanime.builders

import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.Platform
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.time.ZonedDateTime
import java.util.*

class EpisodeVariantMockBuilder {
    var uuid: UUID? = null
    var mapping: EpisodeMapping? = null
    var releaseDateTime: ZonedDateTime? = null
    var platform: Platform? = null
    var audioLocale: String? = null
    var identifier: String? = null
    var url: String? = null
    var uncensored: Boolean = false
    var available: Boolean = true

    fun build(): EpisodeVariant {
        val mockK = mockk<EpisodeVariant>()

        every { mockK.uuid } returns uuid
        every { mockK.mapping } returns mapping
        every { mockK.releaseDateTime } returns (releaseDateTime ?: ZonedDateTime.now())
        every { mockK.platform } returns platform
        every { mockK.audioLocale } returns audioLocale
        every { mockK.identifier } returns identifier
        every { mockK.url } returns url
        every { mockK.uncensored } returns uncensored
        every { mockK.available } returns available
        every { mockK.available = any() } just Runs

        return mockK
    }
}