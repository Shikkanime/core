package fr.shikkanime.jobs

import fr.shikkanime.database.DatabaseManager
import fr.shikkanime.jobs.metadata.AniDbClient
import fr.shikkanime.jobs.metadata.AniListClient
import fr.shikkanime.jobs.metadata.ExternalAnime
import fr.shikkanime.jobs.metadata.MetadataLinker
import fr.shikkanime.jobs.metadata.MyAnimeListClient
import fr.shikkanime.models.MetadataPlatform
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.koin.core.context.GlobalContext

/**
 * Live audit of the metadata linking against the real database and the real metadata APIs.
 *
 * The run resolves every stored anime on AniList, MyAnimeList and AniDB and prints, for each
 * of them, the external entries that were chosen with their titles, year and episode count -
 * the material a manual review needs to confirm every link points at the right entry.
 *
 * It is skipped unless the `RUN_METADATA_LIVE_TESTS` environment variable is set. Nothing is
 * persisted unless `METADATA_LINK_PERSIST=true` is also set, so the audit can run as a dry run
 * first.
 *
 * The linker is invoked through `runBlocking` on purpose: the per-platform rate limiters rely
 * on real delays, which a virtual-time test scheduler would skip and turn into a ban.
 */
@DisplayName("live metadata linking audit")
class MetadataLinkingLiveTest {

    companion object {
        private val koin by lazy {
            GlobalContext.getOrNull() ?: bootstrap()
        }

        private fun assumeLiveTestsEnabled() =
            assumeTrue(
                System.getenv("RUN_METADATA_LIVE_TESTS") == "true",
                "live tests disabled: set RUN_METADATA_LIVE_TESTS=true to run them"
            )
    }

    @Test
    @DisplayName("should link every stored anime and print the chosen entries")
    fun shouldLinkEveryStoredAnimeAndPrintTheChosenEntries() = runBlocking {
        // Given: the real database and the linker resolved by the real dependency injection.
        // The test working directory is the module folder, so the catalog database lives one
        // level up unless JDBC_URL overrides it.
        assumeLiveTestsEnabled()
        val jdbcUrl = System.getenv("JDBC_URL")
            ?: ("jdbc:h2:" + File("..", "data").absolutePath + "/database")
        // The new metadata tables are created on top of the existing catalog.
        val databaseManager = DatabaseManager(jdbcUrl)
        databaseManager.databaseWrapper.connect()
        databaseManager.databaseWrapper.initializeSchema()
        val persist = System.getenv("METADATA_LINK_PERSIST") == "true"
        val linker = koin.get<MetadataLinker>()
        val anilistClient = koin.get<AniListClient>()
        val malClient = koin.get<MyAnimeListClient>()
        val anidbClient = koin.get<AniDbClient>()

        // When: the whole catalog goes through the matching
        val report = linker.linkAll(persist)

        // Then: every anime is reported with its chosen entries for manual verification
        println(
            "animes=${report.animesProcessed} animeLinks=${report.animeLinksSaved} " +
                "episodeLinks=${report.episodeLinksSaved} persist=$persist"
        )
        report.unmatched.forEach { (title, platforms) ->
            println("UNMATCHED $title -> $platforms")
        }
        report.assignments.forEach { (title, platforms) ->
            println("== $title")
            platforms.forEach { (platform, ids) ->
                val entries = resolve(platform, ids, anilistClient, malClient, anidbClient)
                entries.forEach { entry ->
                    println(
                        "   ${platform.name} ${entry.id} | ${entry.titles.joinToString(" / ").take(110)}" +
                            " | ${entry.year} | ${entry.episodeCount ?: "?"} eps"
                    )
                }
            }
        }
    }

    private suspend fun resolve(
        platform: MetadataPlatform,
        ids: Set<String>,
        anilistClient: AniListClient,
        malClient: MyAnimeListClient,
        anidbClient: AniDbClient
    ): List<ExternalAnime> {
        val numericIds = ids.mapNotNull { it.toIntOrNull() }
        return when (platform) {
            MetadataPlatform.ANILIST -> anilistClient.getAnimeByIds(numericIds)
            MetadataPlatform.MYANIMELIST ->
                numericIds.mapNotNull { malClient.getAnimeById(it) }
            MetadataPlatform.ANIDB -> anidbClient.getAnimeByIds(numericIds)
        }
    }
}
