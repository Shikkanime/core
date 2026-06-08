package fr.shikkanime.builders

import fr.shikkanime.wrappers.factories.AbstractDisneyPlusWrapper
import io.mockk.every
import io.mockk.mockk

class DisneyPlusMetadataMockBuilder {
    var showId: String? = null
    var id: String? = null

    fun build(): AbstractDisneyPlusWrapper.Metadata {
        val mockK = mockk<AbstractDisneyPlusWrapper.Metadata>()

        showId?.let { every { mockK.showId } returns it }
        id?.let { every { mockK.id } returns it }

        return mockK
    }
}