package fr.shikkanime.database.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.Locale
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

class AnimeLocalizationMockKBuilder(
    configuration: AnimeLocalizationMockKBuilder.() -> Unit = {}
) : TestBuilder<AnimeLocalization> {
    var id: Uuid? = null
    var createdAt: LocalDateTime? = null
    var updatedAt: LocalDateTime? = null
    var locale: Locale? = null
    var title: String? = null
    var description: String? = null

    init {
        configuration(this)
    }

    override fun build(): AnimeLocalization {
        val mockK = mockk<AnimeLocalization>(relaxed = true)

        id?.let { every { mockK.id } returns it }
        createdAt?.let { every { mockK.createdAt } returns it }
        updatedAt?.let { every { mockK.updatedAt } returns it }
        locale?.let { every { mockK.locale } returns it }
        title?.let { every { mockK.title } returns it }
        description?.let { every { mockK.description } returns it }

        return mockK
    }
}
