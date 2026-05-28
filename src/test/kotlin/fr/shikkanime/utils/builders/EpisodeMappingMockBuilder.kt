package fr.shikkanime.utils.builders

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.EpisodeType
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime

class EpisodeMappingMockBuilder : IMockBuilder<EpisodeMapping> {
    private var anime: Anime? = null
    private var releaseDateTime: ZonedDateTime? = null
    private var season: Int? = null
    private var episodeType: EpisodeType? = null
    private var number: Int? = null

    override fun build(): EpisodeMapping {
        val entity = mockk<EpisodeMapping>(relaxed = true)

        every { entity.uuid } answers { callOriginal() }
        every { entity.anime } returns anime
        every { entity.releaseDateTime } returns (releaseDateTime ?: ZonedDateTime.now())
        every { entity.lastReleaseDateTime } returns entity.releaseDateTime
        every { entity.lastUpdateDateTime } returns entity.releaseDateTime
        every { entity.season } returns season
        every { entity.episodeType } returns episodeType
        every { entity.number } returns number

        return entity
    }

    fun anime(anime: Anime) = apply { this.anime = anime }
    fun releaseDateTime(releaseDateTime: ZonedDateTime) = apply { this.releaseDateTime = releaseDateTime }
    fun season(season: Int) = apply { this.season = season }
    fun episodeType(episodeType: EpisodeType) = apply { this.episodeType = episodeType }
    fun number(number: Int) = apply { this.number = number }
}