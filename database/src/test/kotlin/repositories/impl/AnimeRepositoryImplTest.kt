package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.builders.impl.AnimeLocalizationMockKBuilder
import fr.shikkanime.database.builders.impl.AnimeMockKBuilder
import fr.shikkanime.database.builders.impl.AnimePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.AnimeWithChildrenMockKBuilder
import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.database.repositories.AnimeLocalizationRepository
import fr.shikkanime.database.repositories.AnimePlatformRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.models.AnimeWithChildren
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the anime repository")
class AnimeRepositoryImplTest {
    @MockK private lateinit var imageRepository: ImageRepository
    @MockK private lateinit var localizationRepository: AnimeLocalizationRepository
    @MockK private lateinit var platformRepository: AnimePlatformRepository

    private lateinit var animeRepository: AnimeRepositoryImpl

    @BeforeEach
    fun setUp() {
        val databaseManager = DatabaseManager("jdbc:h2:mem:${Uuid.random()};DB_CLOSE_DELAY=-1")
        databaseManager.databaseWrapper.connect()
        databaseManager.databaseWrapper.initializeSchema()

        every { imageRepository.findAllByEntityIds(any()) } returns emptyMap()
        every { localizationRepository.findAllByAnimeIds(any()) } returns emptyMap()
        every { platformRepository.findAllByAnimeIds(any()) } returns emptyMap()

        // The children are injected lazily: they reference the repository to mint foreign keys
        animeRepository = AnimeRepositoryImpl(
            imageRepository,
            lazyOf(localizationRepository),
            lazyOf(platformRepository)
        )
    }

    private fun savedAnime(): AnimeWithChildren =
        AnimeWithChildrenMockKBuilder {
            anime = AnimeMockKBuilder {
                id = Uuid.random()
                normalizedTitle = "One Piece"
            }.build()
        }.build()

    @Nested
    @DisplayName("tests for the 'save' method")
    inner class SaveTests {
        @Test
        @DisplayName("should create the anime identity row when it does not exist yet")
        fun `should create the anime identity row when it does not exist yet`() {
            // Given
            val anime = savedAnime()

            // When
            val saved = transaction { animeRepository.save(anime) }

            // Then
            val found = transaction { animeRepository.findModelById(requireNotNull(saved.id)) }
            assertEquals(anime.anime.id, assertNotNull(found).anime.id)
        }

        @Test
        @DisplayName("should return the existing anime untouched when the same identifier is saved twice")
        fun `should return the existing anime untouched when the same identifier is saved twice`() {
            // Given
            val anime = savedAnime()
            val first = transaction { animeRepository.save(anime) }

            // When
            val second = transaction { animeRepository.save(anime) }

            // Then
            assertEquals(first.id, second.id)
            assertEquals(first.createdAt, second.createdAt)
        }
    }

    @Nested
    @DisplayName("tests for the 'findModelById' method")
    inner class FindModelByIdTests {
        @Test
        @DisplayName("should return null when the anime does not exist")
        fun `should return null when the anime does not exist`() {
            // Given
            val unknown = Uuid.random()

            // When
            val found = transaction { animeRepository.findModelById(unknown) }

            // Then
            assertNull(found)
        }

        @Test
        @DisplayName("should compose the children of the anime with one batch read per relation")
        fun `should compose the children of the anime with one batch read per relation`() {
            // Given
            val anime = savedAnime()
            val ownerId = anime.anime.id
            val images = listOf(
                ImageMockKBuilder {
                    id = Uuid.random()
                    type = ImageType.BANNER
                    entityId = ownerId
                }.build()
            )
            val localizations = listOf(
                AnimeLocalizationMockKBuilder {
                    id = Uuid.random()
                    animeId = ownerId
                    locale = Locale.FR_FR
                    title = "One Piece"
                    description = "Description"
                }.build()
            )
            val platforms = listOf(
                AnimePlatformMockKBuilder {
                    id = Uuid.random()
                    animeId = ownerId
                    platform = Platform.ANIMATION_DIGITAL_NETWORK
                    platformId = "123"
                }.build()
            )
            every { imageRepository.findAllByEntityIds(any()) } returns mapOf(ownerId to images)
            every { localizationRepository.findAllByAnimeIds(any()) } returns mapOf(ownerId to localizations)
            every { platformRepository.findAllByAnimeIds(any()) } returns mapOf(ownerId to platforms)

            transaction {
                animeRepository.save(anime)
            }

            // When
            val found = assertNotNull(
                transaction { animeRepository.findModelById(requireNotNull(anime.anime.id)) }
            )

            // Then
            assertEquals(ImageType.BANNER, found.images.single().type)
            assertEquals("One Piece", found.localizations.single().title)
            assertEquals("123", found.platforms.single().platformId)
        }

        @Test
        @DisplayName("should return an anime without child when it has none")
        fun `should return an anime without child when it has none`() {
            // Given
            val anime = savedAnime()
            transaction { animeRepository.save(anime) }

            // When
            val found = assertNotNull(
                transaction { animeRepository.findModelById(requireNotNull(anime.anime.id)) }
            )

            // Then
            assertTrue(found.localizations.isEmpty())
            assertTrue(found.platforms.isEmpty())
            assertTrue(found.images.isEmpty())
        }
    }

    @Nested
    @DisplayName("tests for the 'findAllModels' method")
    inner class FindAllTests {
        @Test
        @DisplayName("should return an empty list when the database holds no anime")
        fun `should return an empty list when the database holds no anime`() {
            // Given
            // When
            val found = transaction { animeRepository.findAllModels() }

            // Then
            assertTrue(found.isEmpty())
        }

        @Test
        @DisplayName("should return every stored anime")
        fun `should return every stored anime`() {
            // Given
            val first = savedAnime()
            val second = AnimeWithChildrenMockKBuilder {
                anime = AnimeMockKBuilder {
                    id = Uuid.random()
                    normalizedTitle = "Second"
                }.build()
            }.build()

            transaction {
                animeRepository.save(first)
                animeRepository.save(second)
            }

            // When
            val found = transaction { animeRepository.findAllModels() }

            // Then
            assertEquals(2, found.size)
        }
    }

    @Nested
    @DisplayName("tests for the 'findByPlatformAndPlatformId' method")
    inner class FindByPlatformAndPlatformIdTests {
        @Test
        @DisplayName("should return null when no anime carries the platform link")
        fun `should return null when no anime carries the platform link`() {
            // Given
            // When
            val found = transaction {
                animeRepository.findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "404")
            }

            // Then
            assertNull(found)
        }

        @Test
        @DisplayName("should return the anime owning the platform link")
        fun `should return the anime owning the platform link`() {
            // Given
            val anime = savedAnime()
            transaction {
                animeRepository.save(anime)
                AnimePlatformRepositoryImpl(animeRepository)
            .save(
                    AnimePlatformMockKBuilder {
                        id = Uuid.random()
                        animeId = anime.anime.id
                        platform = Platform.ANIMATION_DIGITAL_NETWORK
                        platformId = "123"
                    }.build()
                )
            }

            // When
            val found = transaction {
                animeRepository.findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "123")
            }

            // Then
            assertEquals(anime.anime.id, assertNotNull(found).anime.id)
        }
    }
}
