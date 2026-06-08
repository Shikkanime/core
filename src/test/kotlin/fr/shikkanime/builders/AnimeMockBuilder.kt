package fr.shikkanime.builders

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.CountryCode
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime
import java.util.*

class AnimeMockBuilder {
    var uuid: UUID? = null
    var countryCode: CountryCode? = null
    var name: String? = null
    var releaseDateTime: ZonedDateTime? = null
    var lastReleaseDateTime: ZonedDateTime? = null
    var lastUpdateDateTime: ZonedDateTime? = null
    var description: String? = null
    var slug: String? = null

    fun build(): Anime {
        val mockK = mockk<Anime>()

        every { mockK.uuid } returns uuid
        every { mockK.countryCode } returns countryCode
        every { mockK.name } returns name
        every { mockK.releaseDateTime } returns (releaseDateTime ?: ZonedDateTime.now())
        every { mockK.lastReleaseDateTime } returns (lastReleaseDateTime ?: mockK.releaseDateTime)
        every { mockK.lastUpdateDateTime } returns (lastUpdateDateTime ?: mockK.releaseDateTime)
        every { mockK.description } returns description
        every { mockK.slug } returns slug

        return mockK
    }
}