# Testing Guide

This guide outlines the conventions and patterns for writing tests in this project. All tests must be written in English.

## Core Libraries

- **[JUnit 5](https://junit.org/junit5/docs/current/user-guide/):** The primary framework for structuring tests.
- **[MockK](https://mockk.io/):** The library used for creating mocks and verifying interactions.

## Test Structure

To ensure tests are organized, readable, and maintainable, follow these structural conventions:

### 1. Group Tests with `@Nested`

Group all tests for a specific function or behavior within an `inner class` annotated with `@Nested`.

```kotlin
class AnimeRepositoryTest {
    @Nested
    @DisplayName("tests for the 'saveAnime' method")
    inner class SaveAnimeTests {
        // ... tests for saveAnime()
    }
}
```

### 2. Use Descriptive Names

- Use `@DisplayName` to provide a human-readable description for test classes and methods.
- Test method names **must** be complete sentences in backticks, describing the expected outcome (e.g., `` `should return an anime when id is valid` ``).

### 3. Follow the Given/When/Then Pattern

Structure your tests logically to separate setup, execution, and assertion. Use comments to delineate the sections.

```kotlin
@Test
fun `should save and retrieve anime`() {
    // Given
    val anime = Anime(name = "One Piece", description = "Pirates")

    // When
    val saved = animeRepository.saveAnime(anime)

    // Then
    assertEquals("One Piece", saved.name)
}
```

### 4. Source Directory Structure

Like production sources, test files and test fixtures must be placed directly under `src/test/kotlin/` or `src/testFixtures/kotlin/` without creating prefix directories for `fr/shikkanime/<module>/`:
- Test classes are placed under subdirectories matching their logical component (e.g., `src/test/kotlin/usecases/CreateAdminUserUseCaseTest.kt` with package `fr.shikkanime.admin.usecases`).
- Test builders are placed under `src/testFixtures/kotlin/builders/` (e.g., `src/testFixtures/kotlin/builders/impl/UserEntityMockKBuilder.kt` with package `fr.shikkanime.database.builders.impl`).

## Mocking with MockK

- Use `@MockK` to create mocks for dependencies.
- Use `@InjectMockKs` to create an instance of the class under test with its dependencies automatically injected.
- The test class **must** be annotated with `@ExtendWith(MockKExtension::class)`.
- Variables with a single annotation (e.g., `@MockK`, `@InjectMockKs`) **must** be placed on a single line.

```kotlin
@ExtendWith(MockKExtension::class)
class AnimeServiceTest {
    @MockK private lateinit var animeRepository: AnimeRepository
    @InjectMockKs private lateinit var animeService: AnimeService

    // ... tests
}
```

## Test Builders

Always use MockK builders implementing `TestBuilder<T>` to instantiate test entities and complex objects instead of calling `mockk<T>()` directly in tests.

Test builders for database entities must reside in `:database` under `src/testFixtures/` so they can be reused across consumer modules via `testImplementation(testFixtures(project(":database")))`.

```kotlin
package fr.shikkanime.database.builders

interface TestBuilder<T> {
    fun build(): T
}
```

Example implementation for an entity:

```kotlin
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
```

Usage in tests:

```kotlin
val adminUser = UserEntityMockKBuilder {
    username = "admin"
    roles = setOf(UserRole.ADMIN)
}.build()
```
