package fr.shikkanime.database.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.models.*
import io.mockk.every
import io.mockk.mockk

class EpisodeWithChildrenMockKBuilder(
    configuration: EpisodeWithChildrenMockKBuilder.() -> Unit = {}
) : TestBuilder<EpisodeWithChildren> {
    var episode: Episode? = null
    var images: List<Image>? = null
    var localizations: List<EpisodeLocalization>? = null
    var releases: List<EpisodeRelease>? = null

    init {
        configuration(this)
    }

    override fun build(): EpisodeWithChildren {
        val mockK = mockk<EpisodeWithChildren>(relaxed = true)

        episode?.let { every { mockK.episode } returns it }
        images?.let { every { mockK.images } returns it }
        localizations?.let { every { mockK.localizations } returns it }
        releases?.let { every { mockK.releases } returns it }

        // The projection delegates read the episode member: a mock intercepts them too
        every { mockK.id } answers { mockK.episode.id }
        every { mockK.createdAt } answers { mockK.episode.createdAt }
        every { mockK.updatedAt } answers { mockK.episode.updatedAt }
        every { mockK.animeId } answers { mockK.episode.animeId }
        every { mockK.season } answers { mockK.episode.season }
        every { mockK.episodeType } answers { mockK.episode.episodeType }
        every { mockK.number } answers { mockK.episode.number }
        every { mockK.releaseDateTime } answers { mockK.episode.releaseDateTime }
        every { mockK.duration } answers { mockK.episode.duration }

        every { mockK.copy(any(), any(), any(), any()) } answers {
            EpisodeWithChildren(
                episode = firstArg(),
                // A named `copy(episode = …)` leaves the other arguments unresolved on a mock
                images = mockK.images,
                localizations = mockK.localizations,
                releases = mockK.releases
            )
        }

        return mockK
    }
}
