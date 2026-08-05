package fr.shikkanime.database.builders.impl

import fr.shikkanime.database.builders.TestBuilder
import fr.shikkanime.database.entities.UserEntity
import fr.shikkanime.models.UserRole
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.crypt.Hashed
import kotlin.uuid.Uuid

class UserEntityMockKBuilder(
    configuration: UserEntityMockKBuilder.() -> Unit = {}
) : TestBuilder<UserEntity> {
    var id: Uuid? = null
    var createdAt: LocalDateTime? = null
    var updatedAt: LocalDateTime? = null
    var identifier: ByteArray? = null
    var username: String? = null
    var password: Hashed? = null
    var roles: Set<UserRole>? = null

    init {
        configuration(this)
    }

    override fun build(): UserEntity {
        val mockK = mockk<UserEntity>(relaxed = true)

        id?.let { every { mockK.id.value } returns it }
        createdAt?.let { every { mockK.createdAt } returns it }
        updatedAt?.let { every { mockK.updatedAt } returns it }
        identifier?.let { every { mockK.identifier } returns it }
        username?.let { every { mockK.username } returns it }
        password?.let { every { mockK.password } returns it }
        roles?.let { every { mockK.roles } returns it }

        return mockK
    }
}
