package fr.shikkanime.database.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.models.Image
import fr.shikkanime.models.ImageType
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

class ImageMockKBuilder(
    configuration: ImageMockKBuilder.() -> Unit = {}
) : TestBuilder<Image> {
    var id: Uuid? = null
    var createdAt: LocalDateTime? = null
    var updatedAt: LocalDateTime? = null
    var entityId: Uuid? = null
    var type: ImageType? = null
    var url: String? = null

    init {
        configuration(this)
    }

    override fun build(): Image {
        val mockK = mockk<Image>(relaxed = true)

        id?.let { every { mockK.id } returns it }
        createdAt?.let { every { mockK.createdAt } returns it }
        updatedAt?.let { every { mockK.updatedAt } returns it }
        entityId?.let { every { mockK.entityId } returns it }
        type?.let { every { mockK.type } returns it }
        url?.let { every { mockK.url } returns it }

        every { mockK.copy(any(), any(), any(), any(), any(), any()) } answers {
            Image(
                id = firstArg(),
                createdAt = secondArg(),
                updatedAt = thirdArg(),
                entityId = arg(3),
                type = arg(4),
                url = arg(5)
            )
        }

        return mockK
    }
}
