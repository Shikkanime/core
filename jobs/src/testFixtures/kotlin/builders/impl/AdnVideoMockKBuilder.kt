package fr.shikkanime.jobs.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.jobs.platforms.impl.AdnShow
import fr.shikkanime.jobs.platforms.impl.AdnType
import fr.shikkanime.jobs.platforms.impl.AdnVideo
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime

/**
 * MockK [TestBuilder] producing [AdnVideo] test fixtures with conditionally stubbed fields.
 *
 * Only the fields set by the test are stubbed, and [AdnVideo.toPlatformEpisode] keeps its real
 * mapping behaviour through `callOriginal`.
 *
 * @param configuration Lambda receiving the builder to set the required fixture fields.
 * @see [AdnVideo]
 * @see [TestBuilder]
 */
internal class AdnVideoMockKBuilder(
    configuration: AdnVideoMockKBuilder.() -> Unit = {}
) : TestBuilder<AdnVideo> {
    var id: Int? = null
    var name: String? = null
    var title: String? = null
    var shortNumber: String? = null
    var season: String? = null
    var type: AdnType? = null
    var image: String? = null
    var summary: String? = null
    var releaseDate: ZonedDateTime? = null
    var duration: Long? = null
    var url: String? = null
    var languages: List<String>? = null
    var show: AdnShow? = null

    init {
        configuration(this)
    }

    override fun build(): AdnVideo {
        val mockK = mockk<AdnVideo>(relaxed = true)
        id?.let { every { mockK.id } returns it }
        name?.let { every { mockK.name } returns it }
        title?.let { every { mockK.title } returns it }
        shortNumber?.let { every { mockK.shortNumber } returns it }
        season?.let { every { mockK.season } returns it }
        type?.let { every { mockK.type } returns it }
        image?.let { every { mockK.image } returns it }
        summary?.let { every { mockK.summary } returns it }
        releaseDate?.let { every { mockK.releaseDate } returns it }
        duration?.let { every { mockK.duration } returns it }
        url?.let { every { mockK.url } returns it }
        languages?.let { every { mockK.languages } returns it }
        show?.let { every { mockK.show } returns it }

        every { mockK.toPlatformEpisode(any(), any()) } answers { callOriginal() }

        return mockK
    }
}
