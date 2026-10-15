package fr.shikkanime.database.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.Platform
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

class AnimePlatformMockKBuilder(
    configuration: AnimePlatformMockKBuilder.() -> Unit = {}
) : TestBuilder<AnimePlatform> {
    var id: Uuid? = null
    var createdAt: LocalDateTime? = null
    var updatedAt: LocalDateTime? = null
    var platform: Platform? = null
    var platformId: String? = null

    init {
        configuration(this)
    }

    override fun build(): AnimePlatform {
        val mockK = mockk<AnimePlatform>(relaxed = true)

        id?.let { every { mockK.id } returns it }
        createdAt?.let { every { mockK.createdAt } returns it }
        updatedAt?.let { every { mockK.updatedAt } returns it }
        platform?.let { every { mockK.platform } returns it }
        platformId?.let { every { mockK.platformId } returns it }

        return mockK
    }
}
