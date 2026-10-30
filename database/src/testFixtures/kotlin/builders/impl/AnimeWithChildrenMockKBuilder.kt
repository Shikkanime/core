package fr.shikkanime.database.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.Image
import io.mockk.every
import io.mockk.mockk

class AnimeWithChildrenMockKBuilder(
    configuration: AnimeWithChildrenMockKBuilder.() -> Unit = {}
) : TestBuilder<AnimeWithChildren> {
    var anime: Anime? = null
    var images: List<Image>? = null
    var localizations: List<AnimeLocalization>? = null
    var platforms: List<AnimePlatform>? = null

    init {
        configuration(this)
    }

    override fun build(): AnimeWithChildren {
        val mockK = mockk<AnimeWithChildren>(relaxed = true)

        anime?.let { every { mockK.anime } returns it }
        images?.let { every { mockK.images } returns it }
        localizations?.let { every { mockK.localizations } returns it }
        platforms?.let { every { mockK.platforms } returns it }

        every { mockK.copy(any(), any(), any(), any()) } answers {
            AnimeWithChildren(
                anime = firstArg(),
                // A named `copy(anime = …)` leaves the other arguments unresolved on a mock
                images = mockK.images,
                localizations = mockK.localizations,
                platforms = mockK.platforms
            )
        }

        return mockK
    }
}
