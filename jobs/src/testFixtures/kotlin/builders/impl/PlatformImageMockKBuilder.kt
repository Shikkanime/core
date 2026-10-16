package fr.shikkanime.jobs.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.jobs.platforms.PlatformImage
import fr.shikkanime.models.ImageType
import io.mockk.every
import io.mockk.mockk

class PlatformImageMockKBuilder(
    configuration: PlatformImageMockKBuilder.() -> Unit = {}
) : TestBuilder<PlatformImage> {
    var type: ImageType? = null
    var url: String? = null

    init {
        configuration(this)
    }

    override fun build(): PlatformImage {
        val mockK = mockk<PlatformImage>(relaxed = true)
        type?.let { every { mockK.type } returns it }
        url?.let { every { mockK.url } returns it }

        every { mockK.toModel() } answers { callOriginal() }

        return mockK
    }
}
