package fr.shikkanime.jobs

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.framework.koin.applyTransactionalProxies
import fr.shikkanime.jobs.platforms.impl.CrunchyrollPlatform
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.koin.core.context.GlobalContext
import org.koin.plugin.module.dsl.startKoin
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/**
 * Live end-to-end runs of the Crunchyroll platform against the real Crunchyroll API.
 *
 * Unlike the mocked unit tests, these runs exercise the whole chain: anonymous token minting,
 * the discover browse endpoint, the batched CMS objects requests, the episode mapping and the
 * persistence of the harvested episodes. They require a network connection and a Crunchyroll
 * API that is not blocking the client.
 */
@DisplayName("live end-to-end tests for the Crunchyroll platform")
class CrunchyrollPlatformLiveTest {

    companion object {
        private val koin by lazy {
            GlobalContext.getOrNull()
                ?: startKoin<MyApp>().koin.also(::applyTransactionalProxies)
        }
    }

    @Test
    @DisplayName("should harvest the latest episodes from the real Crunchyroll API")
    fun shouldHarvestTheLatestEpisodesFromTheRealCrunchyrollApi() = runTest {
        // Given
        val platform = CrunchyrollPlatform(client = SmartHttpClient())

        // When
        val harvest = platform.fetchLatestEpisodes()

        // Then
        println("hasChanged=${harvest.hasChanged}, episodes=${harvest.data.size}")
        harvest.data.take(5).forEach {
            println("- ${it.id} | ${it.anime.localizations.single().title} | ${it.number} | ${it.audioLocales}")
        }
        assertTrue(harvest.data.isNotEmpty(), "no episode harvested from the real API")
    }

    @Test
    @DisplayName("should persist the harvested episodes through the real application bootstrap")
    fun shouldPersistTheHarvestedEpisodesThroughTheRealApplicationBootstrap() = runTest {
        // Given: a fresh database and the platform resolved by the real dependency injection
        freshDatabase()
        val platform = koin.get<CrunchyrollPlatform>()

        // When: the harvest of the real API goes through the ingestion service
        val harvest = platform.fetchLatestEpisodes()
        val saved = koin.get<HarvestIngestionService>().saveAll(listOf(harvest))

        // Then: the harvested animes are persisted and readable back
        println("saved=$saved animes")
        assertTrue(saved > 0, "no anime persisted from the real harvest")

        val stored = koin.get<AnimeService>().findAll()
        assertTrue(stored.isNotEmpty(), "no anime readable from the database")
        stored.take(5).forEach {
            println("- ${it.anime.id} | ${it.localizations.single().title} | ${it.platforms.size} platform(s)")
        }
    }

    /**
     * Boots one fresh in-memory database for a single test.
     *
     * @return the connected database manager, with its schema initialized.
     * @see [DatabaseManager]
     */
    private fun freshDatabase(): DatabaseManager =
        DatabaseManager("jdbc:h2:mem:${Uuid.random()};DB_CLOSE_DELAY=-1").apply {
            databaseWrapper.connect()
            databaseWrapper.initializeSchema()
        }
}
