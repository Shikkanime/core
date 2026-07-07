package fr.shikkanime.builders.impl

import fr.shikkanime.builders.MockKBuilder
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import io.mockk.every
import io.mockk.mockk

class VersionMockKBuilder(
    configuration: VersionMockKBuilder.() -> Unit = {}
) : MockKBuilder<AbstractCrunchyrollWrapper.Version> {
    var audioLocale: String? = null
    var guid: String? = null

    init {
        configuration(this)
    }

    override fun build(): AbstractCrunchyrollWrapper.Version {
        val mockK = mockk<AbstractCrunchyrollWrapper.Version>(relaxed = true)

        audioLocale?.let { every { mockK.audioLocale } returns it }
        guid?.let { every { mockK.guid } returns it }

        return mockK
    }
}