package fr.shikkanime.builders

import fr.shikkanime.wrappers.factories.AbstractDisneyPlusWrapper
import io.mockk.every
import io.mockk.mockk

class DisneyPlusEpisodeMockBuilder {
    var show: AbstractDisneyPlusWrapper.Show? = null
    var id: String? = null
    var oldId: String? = null
    var seasonId: String? = null
    var season: Int? = null
    var number: Int? = null
    var title: String? = null
    var description: String? = null
    var url: String? = null
    var image: String? = null
    var duration: Long? = null
    var resourceId: String? = null
    var audioLocales: Array<String>? = null

    fun build(): AbstractDisneyPlusWrapper.Episode {
        val mockK = mockk<AbstractDisneyPlusWrapper.Episode>()

        show?.let { every { mockK.show } returns it }
        id?.let { every { mockK.id } returns it }
        every { mockK.oldId } returns oldId
        seasonId?.let { every { mockK.seasonId } returns it }
        season?.let { every { mockK.season } returns it }
        number?.let { every { mockK.number } returns it }
        every { mockK.title } returns title
        every { mockK.description } returns description
        url?.let { every { mockK.url } returns it }
        image?.let { every { mockK.image } returns it }
        every { mockK.duration } returns (duration ?: -1L)
        resourceId?.let { every { mockK.resourceId } returns it }
        audioLocales?.let { every { mockK.audioLocales } returns it }

        return mockK
    }
}