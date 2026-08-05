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

## Mocking with MockK

- Use `@MockK` to create mocks for dependencies.
- Use `@InjectMockKs` to create an instance of the class under test with its dependencies automatically injected.
- The test class **must** be annotated with `@ExtendWith(MockKExtension::class)`.

```kotlin
@ExtendWith(MockKExtension::class)
class AnimeServiceTest {
    @MockK private lateinit var animeRepository: AnimeRepository
    @InjectMockKs private lateinit var animeService: AnimeService

    // ... tests
}
```
