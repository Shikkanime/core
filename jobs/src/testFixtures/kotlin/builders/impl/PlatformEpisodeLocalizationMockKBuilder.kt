package fr.shikkanime.jobs.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.jobs.platforms.PlatformEpisodeLocalization
import fr.shikkanime.models.Country
import io.mockk.every
import io.mockk.mockk

class PlatformEpisodeLocalizationMockKBuilder(
    configuration: PlatformEpisodeLocalizationMockKBuilder.() -> Unit = {}
) : TestBuilder<PlatformEpisodeLocalization> {
    var country: Country? = null
    var title: String? = null
    var description: String? = null

    init {
        configuration(this)
    }

    override fun build(): PlatformEpisodeLocalization {
        val mockK = mockk<PlatformEpisodeLocalization>(relaxed = true)
        country?.let { every { mockK.country } returns it }
        title?.let { every { mockK.title } returns it }
        description?.let { every { mockK.description } returns it }

        every { mockK.toModel() } answers { callOriginal() }

        return mockK
    }
}
