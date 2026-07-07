package fr.shikkanime.builders.impl

import fr.shikkanime.builders.MockKBuilder
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import io.mockk.every
import io.mockk.mockk

class BrowseObjectMockKBuilder(
    configuration: BrowseObjectMockKBuilder.() -> Unit = {}
) : MockKBuilder<AbstractCrunchyrollWrapper.BrowseObject> {
    var id: String? = null
    private var episodeMetadata: AbstractCrunchyrollWrapper.Episode? = null

    init {
        configuration(this)
    }

    override fun build(): AbstractCrunchyrollWrapper.BrowseObject {
        val mockK = mockk<AbstractCrunchyrollWrapper.BrowseObject>(relaxed = true)

        id?.let { every { mockK.id } returns it }
        episodeMetadata?.let { every { mockK.episodeMetadata } returns it }

        return mockK
    }

    fun episode(configuration: EpisodeMetadataMockBuilder.() -> Unit = {}) {
        episodeMetadata = EpisodeMetadataMockBuilder(configuration).build()
    }

    class EpisodeMetadataMockBuilder(
        configuration: EpisodeMetadataMockBuilder.() -> Unit = {}
    ) : MockKBuilder<AbstractCrunchyrollWrapper.Episode> {
        var seriesId: String? = null
        var versions: List<AbstractCrunchyrollWrapper.Version>? = null

        init {
            configuration(this)
        }

        override fun build(): AbstractCrunchyrollWrapper.Episode {
            val mockK = mockk<AbstractCrunchyrollWrapper.Episode>(relaxed = true)

            seriesId?.let { every { mockK.seriesId } returns it }
            versions?.let { every { mockK.versions } returns it }

            return mockK
        }
    }
}