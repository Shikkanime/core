package fr.shikkanime.jobs.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.jobs.platforms.impl.AdnShow
import fr.shikkanime.jobs.platforms.impl.AdnType
import io.mockk.every
import io.mockk.mockk

/**
 * MockK [TestBuilder] producing [AdnShow] test fixtures with conditionally stubbed fields.
 *
 * Only the fields set by the test are stubbed, and [AdnShow.toPlatformAnime] keeps its real
 * mapping behaviour through `callOriginal`.
 *
 * @param configuration Lambda receiving the builder to set the required fixture fields.
 * @see [AdnShow]
 * @see [TestBuilder]
 */
internal class AdnShowMockKBuilder(
    configuration: AdnShowMockKBuilder.() -> Unit = {}
) : TestBuilder<AdnShow> {
    var id: Int? = null
    var title: String? = null
    var shortTitle: String? = null
    var originalTitle: String? = null
    var summary: String? = null
    var type: AdnType? = null
    var genres: List<String>? = null
    var thumbnail: String? = null
    var banner: String? = null

    init {
        configuration(this)
    }

    override fun build(): AdnShow {
        val mockK = mockk<AdnShow>(relaxed = true)
        id?.let { every { mockK.id } returns it }
        title?.let { every { mockK.title } returns it }
        shortTitle?.let { every { mockK.shortTitle } returns it }
        originalTitle?.let { every { mockK.originalTitle } returns it }
        summary?.let { every { mockK.summary } returns it }
        type?.let { every { mockK.type } returns it }
        genres?.let { every { mockK.genres } returns it }
        thumbnail?.let { every { mockK.thumbnail } returns it }
        banner?.let { every { mockK.banner } returns it }

        every { mockK.toPlatformAnime(any(), any()) } answers { callOriginal() }

        return mockK
    }
}
