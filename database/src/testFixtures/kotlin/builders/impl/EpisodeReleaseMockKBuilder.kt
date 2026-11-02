package fr.shikkanime.database.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.models.EpisodeRelease
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

class EpisodeReleaseMockKBuilder(
    configuration: EpisodeReleaseMockKBuilder.() -> Unit = {}
) : TestBuilder<EpisodeRelease> {
    var id: Uuid? = null
    var createdAt: LocalDateTime? = null
    var updatedAt: LocalDateTime? = null
    var episodeId: Uuid? = null
    var platform: Platform? = null
    var platformId: String? = null
    var url: String? = null
    var audioLocale: Locale? = null
    var uncensored: Boolean? = null
    var releaseDateTime: LocalDateTime? = null

    init {
        configuration(this)
    }

    override fun build(): EpisodeRelease {
        val mockK = mockk<EpisodeRelease>(relaxed = true)

        id?.let { every { mockK.id } returns it }
        createdAt?.let { every { mockK.createdAt } returns it }
        updatedAt?.let { every { mockK.updatedAt } returns it }
        episodeId?.let { every { mockK.episodeId } returns it }
        platform?.let { every { mockK.platform } returns it }
        platformId?.let { every { mockK.platformId } returns it }
        url?.let { every { mockK.url } returns it }
        audioLocale?.let { every { mockK.audioLocale } returns it }
        uncensored?.let { every { mockK.uncensored } returns it }
        releaseDateTime?.let { every { mockK.releaseDateTime } returns it }

        every {
            mockK.copy(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            EpisodeRelease(
                id = firstArg(),
                createdAt = secondArg(),
                updatedAt = thirdArg(),
                episodeId = arg(3),
                platform = arg(4),
                platformId = arg(5),
                url = arg(6),
                audioLocale = arg(7),
                uncensored = arg(8),
                releaseDateTime = arg(9)
            )
        }

        return mockK
    }
}
