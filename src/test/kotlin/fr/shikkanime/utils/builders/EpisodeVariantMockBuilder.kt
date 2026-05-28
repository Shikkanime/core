package fr.shikkanime.utils.builders

import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.Platform
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime

class EpisodeVariantMockBuilder : IMockBuilder<EpisodeVariant> {
    private var episodeMapping: EpisodeMapping? = null
    private var releaseDateTime: ZonedDateTime? = null
    private var platform: Platform? = null
    private var audioLocale: String? = null
    private var identifier: String? = null
    private var url: String? = null
    private var available: Boolean = true

    override fun build(): EpisodeVariant {
        val entity = mockk<EpisodeVariant>(relaxed = true)

        every { entity.uuid } answers { callOriginal() }
        every { entity.mapping } returns episodeMapping
        every { entity.releaseDateTime } returns (releaseDateTime ?: ZonedDateTime.now())
        every { entity.platform } returns platform
        every { entity.audioLocale } returns audioLocale
        every { entity.identifier } returns identifier
        every { entity.url } returns url
        every { entity.available } returns available

        return entity
    }

    fun mapping(episodeMapping: EpisodeMapping) = apply { this.episodeMapping = episodeMapping }
    fun releaseDateTime(releaseDateTime: ZonedDateTime) = apply { this.releaseDateTime = releaseDateTime }
    fun platform(platform: Platform) = apply { this.platform = platform }
    fun audioLocale(audioLocale: String) = apply { this.audioLocale = audioLocale }
    fun identifier(identifier: String) = apply { this.identifier = identifier }
    fun url(url: String) = apply { this.url = url }
    fun available(available: Boolean) = apply { this.available = available }
}