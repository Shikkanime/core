package fr.shikkanime.utils.builders

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.CountryCode
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime

class AnimeMockBuilder : IMockBuilder<Anime> {
    private var countryCode: CountryCode? = null
    private var name: String? = null
    private var releaseDateTime: ZonedDateTime? = null
    private var slug: String? = null

    override fun build(): Anime {
        val entity = mockk<Anime>(relaxed = true)

        every { entity.countryCode } returns countryCode
        every { entity.name } returns name
        every { entity.releaseDateTime } returns (releaseDateTime ?: ZonedDateTime.now())
        every { entity.lastReleaseDateTime } returns entity.releaseDateTime
        every { entity.lastUpdateDateTime } returns entity.releaseDateTime
        every { entity.slug } returns slug

        return entity
    }

    fun countryCode(countryCode: CountryCode) = apply { this.countryCode = countryCode }
    fun name(name: String) = apply { this.name = name }
    fun releaseDateTime(releaseDateTime: ZonedDateTime) = apply { this.releaseDateTime = releaseDateTime }
    fun slug(slug: String) = apply { this.slug = slug }
}