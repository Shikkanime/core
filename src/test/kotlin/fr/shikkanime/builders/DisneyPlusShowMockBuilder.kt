package fr.shikkanime.builders

import fr.shikkanime.wrappers.factories.AbstractDisneyPlusWrapper
import io.mockk.every
import io.mockk.mockk

class DisneyPlusShowMockBuilder {
    var id: String? = null
    var name: String? = null
    var image: String? = null
    var banner: String? = null
    var carousel: String? = null
    var title: String? = null
    var description: String? = null
    var seasons: Set<String>? = null

    fun build(): AbstractDisneyPlusWrapper.Show {
        val mockK = mockk<AbstractDisneyPlusWrapper.Show>()

        id?.let { every { mockK.id } returns it }
        name?.let { every { mockK.name } returns it }
        image?.let { every { mockK.image } returns it }
        banner?.let { every { mockK.banner } returns it }
        carousel?.let { every { mockK.carousel } returns it }
        title?.let { every { mockK.title } returns it }
        every { mockK.description } returns description
        seasons?.let { every { mockK.seasons } returns it }

        return mockK
    }
}