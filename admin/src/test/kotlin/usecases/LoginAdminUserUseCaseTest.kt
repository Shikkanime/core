package fr.shikkanime.admin.usecases

import fr.shikkanime.admin.usecases.impl.LoginAdminUserUseCaseImpl
import fr.shikkanime.database.builders.impl.UserMockKBuilder
import fr.shikkanime.database.services.UserService
import fr.shikkanime.models.UserRole
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for LoginAdminUserUseCase")
class LoginAdminUserUseCaseTest {
    @MockK private lateinit var userService: UserService
    @InjectMockKs private lateinit var loginAdminUserUseCase: LoginAdminUserUseCaseImpl

    @Nested
    @DisplayName("tests for the 'login' method")
    inner class LoginTests {
        @Test
        @DisplayName("should return the admin dto when the credentials are valid")
        fun `should return the admin dto when the credentials are valid`() {
            // Given
            val userId = Uuid.random()
            val admin = UserMockKBuilder {
                id = userId
                username = "admin"
                roles = setOf(UserRole.ADMIN)
            }.build()
            every { userService.findByUsername("admin") } returns admin
            every { userService.passwordMatches(admin, "secret") } returns true

            // When
            val result = loginAdminUserUseCase.login("admin", "secret")

            // Then
            assertEquals(userId, result.id)
            assertEquals("admin", result.username)
            assertEquals(setOf(UserRole.ADMIN), result.roles)
        }

        @Test
        @DisplayName("should throw when the username is unknown")
        fun `should throw when the username is unknown`() {
            // Given
            every { userService.findByUsername("ghost") } returns null

            // When / Then
            assertThrows<IllegalArgumentException> {
                loginAdminUserUseCase.login("ghost", "secret")
            }
        }

        @Test
        @DisplayName("should throw when the user is not an admin")
        fun `should throw when the user is not an admin`() {
            // Given
            val user = UserMockKBuilder {
                id = Uuid.random()
                username = "viewer"
                roles = emptySet()
            }.build()
            every { userService.findByUsername("viewer") } returns user

            // When / Then
            assertThrows<IllegalArgumentException> {
                loginAdminUserUseCase.login("viewer", "secret")
            }
        }

        @Test
        @DisplayName("should throw when the password does not match")
        fun `should throw when the password does not match`() {
            // Given
            val admin = UserMockKBuilder {
                id = Uuid.random()
                username = "admin"
                roles = setOf(UserRole.ADMIN)
            }.build()
            every { userService.findByUsername("admin") } returns admin
            every { userService.passwordMatches(admin, "wrong") } returns false

            // When / Then
            assertThrows<IllegalArgumentException> {
                loginAdminUserUseCase.login("admin", "wrong")
            }
        }
    }
}
