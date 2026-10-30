package fr.shikkanime.database.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.models.EpisodeLocalization
import fr.shikkanime.models.Locale
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

class EpisodeLocalizationMockKBuilder(
    configuration: EpisodeLocalizationMockKBuilder.() -> Unit = {}
) : TestBuilder<EpisodeLocalization> {
    var id: Uuid? = null
    var createdAt: LocalDateTime? = null
    var updatedAt: LocalDateTime? = null
    var episodeId: Uuid? = null
    var locale: Locale? = null
    var title: String? = null
    var description: String? = null

    init {
        configuration(this)
    }

    override fun build(): EpisodeLocalization {
        val mockK = mockk<EpisodeLocalization>(relaxed = true)

        id?.let { every { mockK.id } returns it }
        createdAt?.let { every { mockK.createdAt } returns it }
        updatedAt?.let { every { mockK.updatedAt } returns it }
        episodeId?.let { every { mockK.episodeId } returns it }
        locale?.let { every { mockK.locale } returns it }
        title?.let { every { mockK.title } returns it }
        description?.let { every { mockK.description } returns it }

        every { mockK.copy(any(), any(), any(), any(), any(), any(), any()) } answers {
            EpisodeLocalization(
                id = firstArg(),
                createdAt = secondArg(),
                updatedAt = thirdArg(),
                episodeId = arg(3),
                locale = arg(4),
                title = arg(5),
                description = arg(6)
            )
        }

        return mockK
    }
}
