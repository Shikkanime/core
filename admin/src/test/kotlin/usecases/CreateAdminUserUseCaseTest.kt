package fr.shikkanime.admin.usecases

import fr.shikkanime.admin.usecases.impl.CreateAdminUserUseCaseImpl
import fr.shikkanime.database.builders.impl.UserEntityMockKBuilder
import fr.shikkanime.database.repositories.UserRepository
import fr.shikkanime.database.services.UserService
import fr.shikkanime.models.UserRole
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@DisplayName("tests for CreateAdminUserUseCase")
class CreateAdminUserUseCaseTest {
    @MockK private lateinit var userRepository: UserRepository
    @MockK private lateinit var userService: UserService
    @InjectMockKs private lateinit var createAdminUserUseCase: CreateAdminUserUseCaseImpl

    @Nested
    @DisplayName("tests for the 'execute' method")
    inner class ExecuteTests {
        @Test
        @DisplayName("should skip admin creation when an admin user already exists")
        fun `should skip admin creation when an admin user already exists`() {
            // Given
            val existingAdmin = UserEntityMockKBuilder {
                roles = setOf(UserRole.ADMIN)
            }.build()
            every { userRepository.findAllByRoles(UserRole.ADMIN) } returns listOf(existingAdmin)

            // When
            createAdminUserUseCase.execute()

            // Then
            verify(exactly = 1) { userRepository.findAllByRoles(UserRole.ADMIN) }
            verify(exactly = 0) { userService.create(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("should create admin user when no admin user exists")
        fun `should create admin user when no admin user exists`() {
            // Given
            val createdUser = UserEntityMockKBuilder {
                username = "admin"
                roles = setOf(UserRole.ADMIN)
            }.build()
            every { userRepository.findAllByRoles(UserRole.ADMIN) } returns emptyList()
            every {
                userService.create(
                    username = "admin",
                    password = any(),
                    roles = setOf(UserRole.ADMIN)
                )
            } returns createdUser

            // When
            createAdminUserUseCase.execute()

            // Then
            verify(exactly = 1) { userRepository.findAllByRoles(UserRole.ADMIN) }
            verify(exactly = 1) {
                userService.create(
                    username = "admin",
                    password = match { it.length == 32 && it.isNotBlank() },
                    roles = setOf(UserRole.ADMIN)
                )
            }
        }
    }
}
