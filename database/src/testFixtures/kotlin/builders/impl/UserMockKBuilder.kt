package fr.shikkanime.database.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.models.User
import fr.shikkanime.models.UserRole
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

class UserMockKBuilder(
    configuration: UserMockKBuilder.() -> Unit = {}
) : TestBuilder<User> {
    var id: Uuid? = null
    var createdAt: LocalDateTime? = null
    var updatedAt: LocalDateTime? = null
    var identifier: List<Byte>? = null
    var username: String? = null
    var roles: Set<UserRole>? = null

    init {
        configuration(this)
    }

    override fun build(): User {
        val mockK = mockk<User>(relaxed = true)

        id?.let { every { mockK.id } returns it }
        createdAt?.let { every { mockK.createdAt } returns it }
        updatedAt?.let { every { mockK.updatedAt } returns it }
        identifier?.let { every { mockK.identifier } returns it }
        username?.let { every { mockK.username } returns it }
        roles?.let { every { mockK.roles } returns it }

        every { mockK.copy(any(), any(), any(), any(), any(), any()) } answers {
            User(
                id = firstArg(),
                createdAt = secondArg(),
                updatedAt = thirdArg(),
                identifier = arg(3),
                username = arg(4),
                roles = arg(5)
            )
        }

        return mockK
    }
}
