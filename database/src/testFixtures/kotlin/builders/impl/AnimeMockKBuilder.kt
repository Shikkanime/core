package fr.shikkanime.database.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.models.Anime
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
    var normalizedTitle: String? = null

    init {
        configuration(this)
    }

    override fun build(): Anime {
        val mockK = mockk<Anime>(relaxed = true)

        id?.let { every { mockK.id } returns it }
        createdAt?.let { every { mockK.createdAt } returns it }
        updatedAt?.let { every { mockK.updatedAt } returns it }
        normalizedTitle?.let { every { mockK.normalizedTitle } returns it }

        every { mockK.copy(any(), any(), any(), any()) } answers {
            Anime(
                id = firstArg(),
                createdAt = secondArg(),
                updatedAt = thirdArg(),
                normalizedTitle = arg(3)
            )
        }

        return mockK
    }
}
