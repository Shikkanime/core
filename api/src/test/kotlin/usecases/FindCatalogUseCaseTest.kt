package fr.shikkanime.api.usecases

import fr.shikkanime.api.usecases.impl.FindCatalogUseCaseImpl
import fr.shikkanime.cache.BinaryCodec
import fr.shikkanime.cache.Cache
import fr.shikkanime.cache.L1Cache
import fr.shikkanime.cache.ValkeyWrapper
import fr.shikkanime.core.TimeUtils
import fr.shikkanime.core.pagination.Page
import fr.shikkanime.core.pagination.PageRequest
import fr.shikkanime.database.cache.CacheBuckets
import fr.shikkanime.database.builders.impl.AnimeLocalizationMockKBuilder
import fr.shikkanime.database.builders.impl.AnimeMockKBuilder
import fr.shikkanime.database.builders.impl.AnimePlatformMockKBuilder
import fr.shikkanime.database.builders.impl.AnimeWithChildrenMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeLocalizationMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeReleaseMockKBuilder
import fr.shikkanime.database.builders.impl.EpisodeWithChildrenMockKBuilder
import fr.shikkanime.database.builders.impl.ImageMockKBuilder
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.database.services.EpisodeService
import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the catalog use case")
class FindCatalogUseCaseTest {
    /**
     * In-memory [ValkeyWrapper] keeping the values and the bucket versions of a real Valkey.
     */
    private class InMemoryValkeyWrapper : ValkeyWrapper {
        private val values = mutableMapOf<String, ByteArray>()
        private val versions = mutableMapOf<String, Long>()
        private val codec = BinaryCodec()

        override suspend fun get(key: String): ByteArray? =
            values[key]
                ?: versions[key]?.let(codec::encodeVersion)

        override suspend fun set(key: String, value: ByteArray, ttl: Duration) {
            values[key] = value
        }

        override suspend fun incr(key: String): Long {
            val next = (versions[key] ?: 0L) + 1L
            versions[key] = next
            return next
        }
    }

    @MockK private lateinit var animeService: AnimeService
    @MockK private lateinit var episodeService: EpisodeService

    private lateinit var cache: Cache
    private lateinit var useCase: FindCatalogUseCaseImpl

    @BeforeEach
    fun setUp() {
        cache = Cache(l1 = L1Cache(), valkeyWrapper = InMemoryValkeyWrapper())
        useCase = FindCatalogUseCaseImpl(animeService, episodeService, cache)
    }

    private val anime = AnimeWithChildrenMockKBuilder {
        anime = AnimeMockKBuilder {
            id = Uuid.random()
            normalizedTitle = "Title"
        }.build()
        images = listOf(
            ImageMockKBuilder {
                id = Uuid.random()
                type = ImageType.BANNER
                url = "https://example.test/artwork.png"
            }.build()
        )
        localizations = listOf(
            AnimeLocalizationMockKBuilder {
                id = Uuid.random()
                locale = Country.FR.defaultLocale
                title = "Title"
            }.build()
        )
        platforms = listOf(
            AnimePlatformMockKBuilder {
                id = Uuid.random()
                platform = Platform.ANIMATION_DIGITAL_NETWORK
                platformId = "123"
            }.build()
        )
    }.build()

    private val episode = EpisodeWithChildrenMockKBuilder {
        episode = EpisodeMockKBuilder {
            id = Uuid.random()
            animeId = Uuid.random()
            season = 1
            number = 1
            episodeType = EpisodeType.EPISODE
            releaseDateTime = TimeUtils.now()
            duration = 1440
        }.build()
        images = listOf(
            ImageMockKBuilder {
                id = Uuid.random()
                type = ImageType.BANNER
                url = "https://example.test/artwork.png"
            }.build()
        )
        localizations = listOf(
            EpisodeLocalizationMockKBuilder {
                id = Uuid.random()
                locale = Locale.FR_FR
                title = "Episode"
            }.build()
        )
        releases = listOf(
            EpisodeReleaseMockKBuilder {
                id = Uuid.random()
                platform = Platform.ANIMATION_DIGITAL_NETWORK
                platformId = "3001"
                url = "https://example.test/episodes/3001"
                audioLocale = Locale.FR_FR
                releaseDateTime = TimeUtils.now()
            }.build()
        )
    }.build()

    @Nested
    @DisplayName("tests for the 'findAnimePage' method")
    inner class FindAnimePageTests {
        @Test
        @DisplayName("should serve the second identical page read from the cache")
        fun `should serve the second identical page read from the cache`() =
            runBlocking {
                // Given
                every { animeService.findPage(any()) } returns Page(listOf(anime), 1, 20, 1)

                // When
                val first = useCase.findAnimePage(PageRequest(1, 20))
                val second = useCase.findAnimePage(PageRequest(1, 20))

                // Then
                assertEquals(first, second)
                verify(exactly = 1) { animeService.findPage(any()) }
            }

        @Test
        @DisplayName("should keep a distinct entry for each requested page")
        fun `should keep a distinct entry for each requested page`() =
            runBlocking {
                // Given
                every { animeService.findPage(any()) } returns Page(listOf(anime), 1, 20, 1)

                // When
                useCase.findAnimePage(PageRequest(1, 20))
                useCase.findAnimePage(PageRequest(2, 20))

                // Then
                verify(exactly = 2) { animeService.findPage(any()) }
            }

        @Test
        @DisplayName("should read a page written by another instance from Valkey")
        fun `should read a page written by another instance from Valkey`() =
            runBlocking {
                // Given
                val valkeyWrapper = InMemoryValkeyWrapper()
                val firstUseCase = FindCatalogUseCaseImpl(
                    animeService,
                    episodeService,
                    Cache(l1 = L1Cache(), valkeyWrapper = valkeyWrapper)
                )
                val secondUseCase = FindCatalogUseCaseImpl(
                    animeService,
                    episodeService,
                    Cache(l1 = L1Cache(), valkeyWrapper = valkeyWrapper)
                )
                every { animeService.findPage(any()) } returns Page(listOf(anime), 1, 20, 1)

                // When
                firstUseCase.findAnimePage(PageRequest(1, 20))
                val page = secondUseCase.findAnimePage(PageRequest(1, 20))

                // Then
                assertEquals(1, page.data.size)
                verify(exactly = 1) { animeService.findPage(any()) }
            }

        @Test
        @DisplayName("should reload the page after the anime bucket is invalidated")
        fun `should reload the page after the anime bucket is invalidated`() =
            runBlocking {
                // Given
                every { animeService.findPage(any()) } returns Page(listOf(anime), 1, 20, 1)

                // When
                useCase.findAnimePage(PageRequest(1, 20))
                cache.invalidate(CacheBuckets.ANIME)
                useCase.findAnimePage(PageRequest(1, 20))

                // Then
                verify(exactly = 2) { animeService.findPage(any()) }
            }
    }

    @Nested
    @DisplayName("tests for the 'findAnime' method")
    inner class FindAnimeTests {
        @Test
        @DisplayName("should serve the second read of the same anime from the cache")
        fun `should serve the second read of the same anime from the cache`() =
            runBlocking {
                // Given
                val id = requireNotNull(anime.anime.id)
                every { animeService.findById(id) } returns anime

                // When
                useCase.findAnime(id)
                useCase.findAnime(id)

                // Then
                verify(exactly = 1) { animeService.findById(id) }
            }

        @Test
        @DisplayName("should keep a distinct entry for each anime")
        fun `should keep a distinct entry for each anime`() =
            runBlocking {
                // Given
                val otherId = Uuid.random()
                every { animeService.findById(any()) } returns anime

                // When
                useCase.findAnime(requireNotNull(anime.anime.id))
                useCase.findAnime(otherId)

                // Then
                verify(exactly = 2) { animeService.findById(any()) }
            }
    }

    @Nested
    @DisplayName("tests for the 'findEpisodePage' method")
    inner class FindEpisodePageTests {
        @Test
        @DisplayName("should serve the second identical page read from the cache")
        fun `should serve the second identical page read from the cache`() =
            runBlocking {
                // Given
                val animeId = Uuid.random()
                every { episodeService.findPageByAnimeId(any(), any()) } returns
                    Page(listOf(episode), 1, 20, 1)

                // When
                val first = useCase.findEpisodePage(animeId, PageRequest(1, 20))
                val second = useCase.findEpisodePage(animeId, PageRequest(1, 20))

                // Then
                assertEquals(first, second)
                verify(exactly = 1) { episodeService.findPageByAnimeId(any(), any()) }
            }

        @Test
        @DisplayName("should keep a distinct entry for each anime and page")
        fun `should keep a distinct entry for each anime and page`() =
            runBlocking {
                // Given
                every { episodeService.findPageByAnimeId(any(), any()) } returns
                    Page(listOf(episode), 1, 20, 1)

                // When
                useCase.findEpisodePage(Uuid.random(), PageRequest(1, 20))
                useCase.findEpisodePage(Uuid.random(), PageRequest(1, 20))

                // Then
                verify(exactly = 2) { episodeService.findPageByAnimeId(any(), any()) }
            }
    }

    @Nested
    @DisplayName("tests for the 'findEpisode' method")
    inner class FindEpisodeTests {
        @Test
        @DisplayName("should serve the second read of the same episode from the cache")
        fun `should serve the second read of the same episode from the cache`() =
            runBlocking {
                // Given
                val id = requireNotNull(episode.episode.id)
                every { episodeService.findById(id) } returns episode

                // When
                useCase.findEpisode(id)
                useCase.findEpisode(id)

                // Then
                verify(exactly = 1) { episodeService.findById(id) }
            }
    }
}
