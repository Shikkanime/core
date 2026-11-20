package fr.shikkanime.database.repositories.impl

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.entities.AnimeEntity
import fr.shikkanime.database.entities.EpisodeEntity
import fr.shikkanime.database.entities.EpisodeReleaseEntity
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@DisplayName("tests for the episode group repository")
class EpisodeGroupRepositoryImplTest {
    private val repository = EpisodeGroupRepositoryImpl()

    @BeforeEach
    fun setUp() {
        val databaseManager = DatabaseManager("jdbc:h2:mem:${Uuid.random()};DB_CLOSE_DELAY=-1")
        databaseManager.databaseWrapper.connect()
        databaseManager.databaseWrapper.initializeSchema()
    }

    private fun seedEpisode(
        animeTitle: String = "Test Anime",
        season: Int = 1,
        number: Int = 1,
        releases: List<Triple<Locale, LocalDateTime, String>> = emptyList()
    ): Pair<Uuid, Uuid> = transaction {
        val anime = AnimeEntity.new { normalizedTitle = animeTitle }
        val episode = EpisodeEntity.new {
            this.anime = anime.id
            this.season = season
            episodeType = EpisodeType.EPISODE
            this.number = number
            releaseDateTime = LocalDateTime.parse("2026-01-01T00:00:00")
            duration = 0
        }
        releases.forEach { (locale, date, platformId) ->
            EpisodeReleaseEntity.new {
                this.episode = episode.id
                platform = Platform.CRUNCHYROLL
                this.platformId = platformId
                url = "https://example.test/$platformId"
                audioLocale = locale
                uncensored = false
                releaseDateTime = date
            }
        }
        anime.id.value to episode.id.value
    }

    @Nested
    @DisplayName("tests for the 'findObservations' method")
    inner class FindObservationsTests {
        @Test
        @DisplayName("should map release rows to flat observations")
        fun `should map release rows to flat observations`() {
            // Given
            val date = LocalDateTime.parse("2026-02-01T10:00:00")
            val (animeId, episodeId) = seedEpisode(
                releases = listOf(Triple(Locale.JA_JP, date, "v1"), Triple(Locale.FR_FR, date, "v2"))
            )

            // When
            val found = transaction {
                repository.findObservations(
                    from = LocalDateTime.parse("2026-01-01T00:00:00"),
                    to = null,
                    animeId = null,
                    episodeType = null
                )
            }

            // Then
            assertEquals(2, found.size)
            val observation = found.first { it.audioLocale == Locale.JA_JP }
            assertEquals(animeId, observation.animeId)
            assertEquals("Test Anime", observation.animeSlug)
            assertEquals(episodeId, observation.episodeId)
            assertEquals(1, observation.season)
            assertEquals(1, observation.number)
            assertEquals(date, observation.releaseDateTime)
        }

        @Test
        @DisplayName("should bound observations by the release date window")
        fun `should bound observations by the release date window`() {
            // Given
            seedEpisode(
                releases = listOf(
                    Triple(Locale.JA_JP, LocalDateTime.parse("2026-01-10T00:00:00"), "old"),
                    Triple(Locale.JA_JP, LocalDateTime.parse("2026-03-10T00:00:00"), "new")
                )
            )

            // When
            val found = transaction {
                repository.findObservations(
                    from = LocalDateTime.parse("2026-02-01T00:00:00"),
                    to = LocalDateTime.parse("2026-02-28T00:00:00"),
                    animeId = null,
                    episodeType = null
                )
            }

            // Then
            assertTrue(found.isEmpty())
        }
    }

    @Nested
    @DisplayName("tests for the 'findAudioLocalesByEpisodeIds' method")
    inner class FindAudioLocalesTests {
        @Test
        @DisplayName("should group every locale of each episode whatever its date")
        fun `should group every locale of each episode whatever its date`() {
            // Given
            val (_, episodeId) = seedEpisode(
                releases = listOf(
                    Triple(Locale.EN_US, LocalDateTime.parse("2025-01-01T00:00:00"), "en"),
                    Triple(Locale.JA_JP, LocalDateTime.parse("2026-06-01T00:00:00"), "ja")
                )
            )

            // When
            val found = transaction {
                repository.findAudioLocalesByEpisodeIds(listOf(episodeId))
            }

            // Then
            assertEquals(setOf(Locale.EN_US, Locale.JA_JP), found[episodeId])
        }
    }

    @Nested
    @DisplayName("tests for the 'findReleasesByIds' method")
    inner class FindReleasesByIdsTests {
        @Test
        @DisplayName("should load releases keyed by identifier")
        fun `should load releases keyed by identifier`() {
            // Given
            val (_, episodeId) = seedEpisode(
                releases = listOf(Triple(Locale.JA_JP, LocalDateTime.parse("2026-01-05T00:00:00"), "v1"))
            )
            val releaseId = transaction {
                fr.shikkanime.database.entities.EpisodeReleaseEntity.all()
                    .first { it.episode.value == episodeId }
                    .id.value
            }

            // When
            val found = transaction { repository.findReleasesByIds(listOf(releaseId)) }

            // Then
            assertEquals("v1", found[releaseId]?.platformId)
            assertEquals(Platform.CRUNCHYROLL, found[releaseId]?.platform)
        }
    }
}
