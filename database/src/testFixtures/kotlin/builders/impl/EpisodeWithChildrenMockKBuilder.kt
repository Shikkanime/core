package fr.shikkanime.database.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.models.Episode
import fr.shikkanime.models.EpisodeLocalization
import fr.shikkanime.models.EpisodePlatform
import fr.shikkanime.models.EpisodeWithChildren
import fr.shikkanime.models.Image
import io.mockk.every
import io.mockk.mockk

class EpisodeWithChildrenMockKBuilder(
    configuration: EpisodeWithChildrenMockKBuilder.() -> Unit = {}
) : TestBuilder<EpisodeWithChildren> {
    var episode: Episode? = null
    var images: List<Image>? = null
    var localizations: List<EpisodeLocalization>? = null
    var platforms: List<EpisodePlatform>? = null

    init {
        configuration(this)
    }

    override fun build(): EpisodeWithChildren {
        val mockK = mockk<EpisodeWithChildren>(relaxed = true)

        episode?.let { every { mockK.episode } returns it }
        images?.let { every { mockK.images } returns it }
        localizations?.let { every { mockK.localizations } returns it }
        platforms?.let { every { mockK.platforms } returns it }

        every { mockK.copy(any(), any(), any(), any()) } answers {
            EpisodeWithChildren(
                episode = firstArg(),
                // A named `copy(episode = …)` leaves the other arguments unresolved on a mock
                images = mockK.images,
                localizations = mockK.localizations,
                platforms = mockK.platforms
            )
        }

        return mockK
    }
}
