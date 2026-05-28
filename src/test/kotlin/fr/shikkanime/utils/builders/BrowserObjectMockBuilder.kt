package fr.shikkanime.utils.builders

import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime

class BrowserObjectMockBuilder : IMockBuilder<AbstractCrunchyrollWrapper.BrowseObject> {
    private var id: String? = null
    private var slugTitle: String? = null
    private var title: String? = null
    private var description: String? = null

    override fun build() = buildInternal()

    internal fun buildInternal(
        episode: AbstractCrunchyrollWrapper.Episode? = null,
        series: AbstractCrunchyrollWrapper.Series? = null
    ): AbstractCrunchyrollWrapper.BrowseObject {
        val entity = mockk<AbstractCrunchyrollWrapper.BrowseObject>(relaxed = true)

        every { entity.id } returns requireNotNull(id)
        every { entity.slugTitle } returns slugTitle
        every { entity.title } returns title
        every { entity.description } returns description
        every { entity.episodeMetadata } returns episode
        every { entity.seriesMetadata } returns series

        return entity
    }

    fun id(id: String) = apply { this.id = id }
    fun slugTitle(slugTitle: String) = apply { this.slugTitle = slugTitle }
    fun title(title: String) = apply { this.title = title }
    fun description(description: String) = apply { this.description = description }
    fun episode() = EpisodeBuilder(this)
    fun series() = SeriesBuilder(this)

    class EpisodeBuilder(private val parent: BrowserObjectMockBuilder) : IMockBuilder<AbstractCrunchyrollWrapper.BrowseObject> {
        private var seriesId: String? = null
        private var seriesTitle: String? = null
        private var audioLocale: String? = null
        private var subtitleLocales: List<String>? = null
        private var premiumAvailableDate: ZonedDateTime? = null
        private var seasonId: String? = null
        private var durationMs: Long? = null
        private var matureBlocked: Boolean = false
        private var numberString: String? = null

        override fun build(): AbstractCrunchyrollWrapper.BrowseObject {
            val entity = mockk<AbstractCrunchyrollWrapper.Episode>(relaxed = true)

            every { entity.seriesId } returns requireNotNull(seriesId)
            every { entity.seriesTitle } returns requireNotNull(seriesTitle)
            every { entity.audioLocale } returns requireNotNull(audioLocale)
            every { entity.subtitleLocales } returns requireNotNull(subtitleLocales)
            every { entity.premiumAvailableDate } returns requireNotNull(premiumAvailableDate)
            every { entity.seasonId } returns requireNotNull(seasonId)
            every { entity.durationMs } returns requireNotNull(durationMs)
            every { entity.matureBlocked } returns matureBlocked
            every { entity.numberString } returns requireNotNull(numberString)

            return parent.buildInternal(episode = entity)
        }

        fun seriesId(seriesId: String) = apply { this.seriesId = seriesId }
        fun seriesTitle(seriesTitle: String) = apply { this.seriesTitle = seriesTitle }
        fun audioLocale(audioLocale: String) = apply { this.audioLocale = audioLocale }
        fun subtitleLocales(subtitleLocales: List<String>) = apply { this.subtitleLocales = subtitleLocales }
        fun premiumAvailableDate(premiumAvailableDate: ZonedDateTime) = apply { this.premiumAvailableDate = premiumAvailableDate }
        fun seasonId(seasonId: String) = apply { this.seasonId = seasonId }
        fun durationMs(durationMs: Long) = apply { this.durationMs = durationMs }
        fun matureBlocked(matureBlocked: Boolean) = apply { this.matureBlocked = matureBlocked }
        fun numberString(numberString: String) = apply { this.numberString = numberString }
    }

    class SeriesBuilder(private val parent: BrowserObjectMockBuilder) : IMockBuilder<AbstractCrunchyrollWrapper.BrowseObject> {
        private var isSimulcast: Boolean = false

        override fun build(): AbstractCrunchyrollWrapper.BrowseObject {
            val entity = mockk<AbstractCrunchyrollWrapper.Series>(relaxed = true)

            every { entity.isSimulcast } returns isSimulcast

            return parent.buildInternal(series = entity)
        }

        fun isSimulcast(isSimulcast: Boolean) = apply { this.isSimulcast = isSimulcast }
    }
}