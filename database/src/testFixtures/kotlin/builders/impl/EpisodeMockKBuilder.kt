package fr.shikkanime.database.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.models.Episode
import fr.shikkanime.models.EpisodeType
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

class EpisodeMockKBuilder(
    configuration: EpisodeMockKBuilder.() -> Unit = {}
) : TestBuilder<Episode> {
    var id: Uuid? = null
    var createdAt: LocalDateTime? = null
    var updatedAt: LocalDateTime? = null
    var animeId: Uuid? = null
    var season: Int? = null
    var number: Int? = null
    var episodeType: EpisodeType? = null
    var releaseDateTime: LocalDateTime? = null
    var duration: Long? = null

    init {
        configuration(this)
    }

    override fun build(): Episode {
        val mockK = mockk<Episode>(relaxed = true)

        // The identifier is always stubbed: a null one means "not persisted yet" to the services
        every { mockK.id } returns id
        createdAt?.let { every { mockK.createdAt } returns it }
        updatedAt?.let { every { mockK.updatedAt } returns it }
        animeId?.let { every { mockK.animeId } returns it }
        season?.let { every { mockK.season } returns it }
        number?.let { every { mockK.number } returns it }
        episodeType?.let { every { mockK.episodeType } returns it }
        releaseDateTime?.let { every { mockK.releaseDateTime } returns it }
        duration?.let { every { mockK.duration } returns it }

        every {
            mockK.copy(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            Episode(
                id = firstArg(),
                createdAt = secondArg(),
                updatedAt = thirdArg(),
                animeId = arg(3),
                season = arg(4),
                number = arg(5),
                episodeType = arg(6),
                releaseDateTime = arg(7),
                duration = arg(8)
            )
        }

        return mockK
    }
}
