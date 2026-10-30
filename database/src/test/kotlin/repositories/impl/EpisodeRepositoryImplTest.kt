package fr.shikkanime.database.repositories.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.builders.impl.EpisodeLocalizationMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeWithChildrenMockKBuilder
import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.database.builders.impl.savedAnime
import fr.shikkanime.database.entities.AnimeTable
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.database.repositories.EpisodeLocalizationRepository
import fr.shikkanime.database.repositories.EpisodePlatformRepository
import fr.shikkanime.database.repositories.ImageRepository
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.EpisodeWithChildren
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.EntityID
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
@DisplayName("tests for the episode repository")
class EpisodeRepositoryImplTest {
    @MockK private lateinit var animeRepository: AnimeRepository
    @MockK private lateinit var imageRepository: ImageRepository
    @MockK private lateinit var localizationRepository: EpisodeLocalizationRepository
    @MockK private lateinit var platformRepository: EpisodePlatformRepository

    private lateinit var episodeRepository: EpisodeRepositoryImpl

    @BeforeEach
    fun setUp() {
        val databaseManager = DatabaseManager("jdbc:h2:mem:${Uuid.random()};DB_CLOSE_DELAY=-1")
        databaseManager.databaseWrapper.connect()
        databaseManager.databaseWrapper.initializeSchema()

        every { animeRepository.getReference(any()) } answers { EntityID(firstArg(), AnimeTable) }
        every { imageRepository.findAllByEntityIds(any()) } returns emptyMap()
        every { localizationRepository.findAllByEpisodeIds(any()) } returns emptyMap()
        every { platformRepository.findAllByEpisodeIds(any()) } returns emptyMap()

        // The children are injected directly: they resolve the episode reference from the model
        episodeRepository = EpisodeRepositoryImpl(
            animeRepository,
            imageRepository,
            localizationRepository,
            platformRepository
        )
    }

    private fun episodeFixture(
        animeId: Uuid,
        number: Int = 1,
        releaseDateTime: LocalDateTime = TimeUtils.now()
    ): EpisodeWithChildren =
        EpisodeWithChildrenMockKBuilder {
            episode = EpisodeMockKBuilder {
                id = Uuid.random()
                this.animeId = animeId
                season = 1
                this.number = number
                episodeType = EpisodeType.EPISODE
                this.releaseDateTime = releaseDateTime
                duration = 1440
            }.build()
        }.build()

    @Nested
    @DisplayName("tests for the 'save' method")
    inner class SaveTests {
        @Test
        @DisplayName("should create the episode identity row when it does not exist yet")
        fun `should create the episode identity row when it does not exist yet`() {
            // Given
            val anime = savedAnime()
            val episode = episodeFixture(requireNotNull(anime.id))

            // When
            val saved = transaction { episodeRepository.save(episode) }

            // Then
            val found = transaction { episodeRepository.findModelById(requireNotNull(saved.id)) }
            assertEquals(episode.episode.id, assertNotNull(found).episode.id)
        }

        @Test
        @DisplayName("should return the existing episode untouched when the same identifier is saved twice")
        fun `should return the existing episode untouched when the same identifier is saved twice`() {
            // Given
            val anime = savedAnime()
            val episode = episodeFixture(requireNotNull(anime.id))
            val first = transaction { episodeRepository.save(episode) }

            // When
            val second = transaction { episodeRepository.save(episode) }

            // Then
            assertEquals(first.id, second.id)
            assertEquals(first.createdAt, second.createdAt)
        }

        @Test
        @DisplayName("should update the metadata when the same episode is saved with new values")
        fun `should update the metadata when the same episode is saved with new values`() {
            // Given
            val anime = savedAnime()
            val episode = episodeFixture(requireNotNull(anime.id))
            transaction { episodeRepository.save(episode) }
            val changed = EpisodeWithChildrenMockKBuilder {
                this.episode = EpisodeMockKBuilder {
                    id = episode.episode.id
                    animeId = episode.episode.animeId
                    season = 1
                    number = 2
                    episodeType = EpisodeType.EPISODE
                    releaseDateTime = TimeUtils.now()
                    duration = 2880
                }.build()
            }.build()

            // When
            val saved = transaction { episodeRepository.save(changed) }

            // Then
            assertEquals(2, saved.number)
            assertEquals(2880, saved.duration)
        }
    }

    @Nested
    @DisplayName("tests for the 'findModelById' method")
    inner class FindModelByIdTests {
        @Test
        @DisplayName("should return null when the episode does not exist")
        fun `should return null when the episode does not exist`() {
            // Given
            val unknown = Uuid.random()

            // When
            val found = transaction { episodeRepository.findModelById(unknown) }

            // Then
            assertNull(found)
        }

        @Test
        @DisplayName("should compose the children of the episode with one batch read per relation")
        fun `should compose the children of the episode with one batch read per relation`() {
            // Given
            val anime = savedAnime()
            val episode = episodeFixture(requireNotNull(anime.id))
            val ownerId = episode.episode.id
            val images = listOf(
                ImageMockKBuilder {
                    id = Uuid.random()
                    type = ImageType.BANNER
                    entityId = ownerId
                }.build()
            )
            val localizations = listOf(
                EpisodeLocalizationMockKBuilder {
                    id = Uuid.random()
                    episodeId = ownerId
                    locale = Locale.FR_FR
                    title = "Episode"
                    description = "Description"
                }.build()
            )
            val platforms = listOf(
                EpisodePlatformMockKBuilder {
                    id = Uuid.random()
                    episodeId = ownerId
                    platform = Platform.ANIMATION_DIGITAL_NETWORK
                    platformId = "3001"
                }.build()
            )
            every { imageRepository.findAllByEntityIds(any()) } returns mapOf(ownerId to images)
            every { localizationRepository.findAllByEpisodeIds(any()) } returns mapOf(ownerId to localizations)
            every { platformRepository.findAllByEpisodeIds(any()) } returns mapOf(ownerId to platforms)

            transaction {
                episodeRepository.save(episode)
            }

            // When
            val found = assertNotNull(
                transaction { episodeRepository.findModelById(requireNotNull(episode.episode.id)) }
            )

            // Then
            assertEquals(ImageType.BANNER, found.images.single().type)
            assertEquals("Episode", found.localizations.single().title)
            assertEquals("3001", found.platforms.single().platformId)
        }

        @Test
        @DisplayName("should return an episode without child when it has none")
        fun `should return an episode without child when it has none`() {
            // Given
            val anime = savedAnime()
            val episode = episodeFixture(requireNotNull(anime.id))
            transaction { episodeRepository.save(episode) }

            // When
            val found = assertNotNull(
                transaction { episodeRepository.findModelById(requireNotNull(episode.episode.id)) }
            )

            // Then
            assertTrue(found.localizations.isEmpty())
            assertTrue(found.platforms.isEmpty())
            assertTrue(found.images.isEmpty())
        }
    }

    @Nested
    @DisplayName("tests for the 'findAllByAnimeId' method")
    inner class FindAllByAnimeIdTests {
        @Test
        @DisplayName("should return an empty list when the anime holds no episode")
        fun `should return an empty list when the anime holds no episode`() {
            // Given
            val anime = savedAnime()

            // When
            val found = transaction { episodeRepository.findAllByAnimeId(requireNotNull(anime.id)) }

            // Then
            assertTrue(found.isEmpty())
        }

        @Test
        @DisplayName("should return the episodes of the anime in broadcast order")
        fun `should return the episodes of the anime in broadcast order`() {
            // Given
            val anime = savedAnime()
            val animeId = requireNotNull(anime.id)
            val second = episodeFixture(animeId, number = 2)
            val first = episodeFixture(animeId, number = 1)
            transaction {
                episodeRepository.save(second)
                episodeRepository.save(first)
            }

            // When
            val found = transaction { episodeRepository.findAllByAnimeId(animeId) }

            // Then
            assertEquals(listOf(1, 2), found.map { it.episode.number })
        }
    }

    @Nested
    @DisplayName("tests for the 'findByAnimeIdAndSeasonAndNumberAndEpisodeType' method")
    inner class FindByAnimeIdAndSeasonAndNumberAndEpisodeTypeTests {
        @Test
        @DisplayName("should return null when no episode carries the identity")
        fun `should return null when no episode carries the identity`() {
            // Given
            val anime = savedAnime()

            // When
            val found = transaction {
                episodeRepository.findByAnimeIdAndSeasonAndNumberAndEpisodeType(
                    requireNotNull(anime.id),
                    1,
                    1,
                    EpisodeType.EPISODE
                )
            }

            // Then
            assertNull(found)
        }

        @Test
        @DisplayName("should return the episode carrying the identity")
        fun `should return the episode carrying the identity`() {
            // Given
            val anime = savedAnime()
            val episode = episodeFixture(requireNotNull(anime.id))
            transaction { episodeRepository.save(episode) }

            // When
            val found = transaction {
                episodeRepository.findByAnimeIdAndSeasonAndNumberAndEpisodeType(
                    requireNotNull(anime.id),
                    1,
                    1,
                    EpisodeType.EPISODE
                )
            }

            // Then
            assertEquals(episode.episode.id, assertNotNull(found).episode.id)
        }
    }

    @Nested
    @DisplayName("tests for the 'findByPlatformAndPlatformId' method")
    inner class FindByPlatformAndPlatformIdTests {
        @Test
        @DisplayName("should return null when no episode carries the platform release")
        fun `should return null when no episode carries the platform release`() {
            // Given
            // When
            val found = transaction {
                episodeRepository.findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "404")
            }

            // Then
            assertNull(found)
        }

        @Test
        @DisplayName("should return the episode owning the platform release")
        fun `should return the episode owning the platform release`() {
            // Given
            val anime = savedAnime()
            val episode = episodeFixture(requireNotNull(anime.id))
            transaction {
                episodeRepository.save(episode)
                EpisodePlatformRepositoryImpl()
                    .save(
                        EpisodePlatformMockKBuilder {
                            id = Uuid.random()
                            episodeId = episode.episode.id
                            platform = Platform.ANIMATION_DIGITAL_NETWORK
                            platformId = "3001"
                            url = "https://example.test/episodes/3001"
                            audioLocales = setOf(Locale.FR_FR)
                            uncensored = false
                            releaseDateTime = TimeUtils.now()
                        }.build()
                    )
            }

            // When
            val found = transaction {
                episodeRepository.findByPlatformAndPlatformId(Platform.ANIMATION_DIGITAL_NETWORK, "3001")
            }

            // Then
            assertEquals(episode.episode.id, assertNotNull(found).episode.id)
        }
    }
}
