package fr.shikkanime.jobs.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformAnimeLocalization
import fr.shikkanime.jobs.platforms.PlatformImage
import io.mockk.every
import io.mockk.mockk

class PlatformAnimeMockKBuilder(
    configuration: PlatformAnimeMockKBuilder.() -> Unit = {}
) : TestBuilder<PlatformAnime> {
    var id: String? = null
    var images: List<PlatformImage>? = null
    var localizations: List<PlatformAnimeLocalization>? = null

    init {
        configuration(this)
    }

    override fun build(): PlatformAnime {
        val mockK = mockk<PlatformAnime>(relaxed = true)
        id?.let { every { mockK.id } returns it }
        images?.let { every { mockK.images } returns it }
        localizations?.let { every { mockK.localizations } returns it }

        every { mockK.toModel(any(), any()) } answers { callOriginal() }

        return mockK
    }
}
