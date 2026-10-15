package fr.shikkanime.database.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.Image
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

class AnimeMockKBuilder(
    configuration: AnimeMockKBuilder.() -> Unit = {}
) : TestBuilder<Anime> {
    var id: Uuid? = null
    var createdAt: LocalDateTime? = null
    var updatedAt: LocalDateTime? = null
    var images: List<Image>? = null
    var localizations: List<AnimeLocalization>? = null
    var platforms: List<AnimePlatform>? = null

    init {
        configuration(this)
    }

    override fun build(): Anime {
        val mockK = mockk<Anime>(relaxed = true)

        id?.let { every { mockK.id } returns it }
        createdAt?.let { every { mockK.createdAt } returns it }
        updatedAt?.let { every { mockK.updatedAt } returns it }
        images?.let { every { mockK.images } returns it }
        localizations?.let { every { mockK.localizations } returns it }
        platforms?.let { every { mockK.platforms } returns it }

        every { mockK.copy(any(), any(), any(), any(), any(), any()) } answers {
            Anime(
                id = firstArg(),
                createdAt = secondArg(),
                updatedAt = thirdArg(),
                images = arg(3),
                localizations = arg(4),
                platforms = arg(5)
            )
        }

        return mockK
    }
}
