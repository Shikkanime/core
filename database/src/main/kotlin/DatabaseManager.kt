package fr.shikkanime.database

import fr.shikkanime.database.entities.*
import fr.shikkanime.exposed.DatabaseWrapper
import java.io.File

/**
 * Owns the connection settings and the registered schema of the database.
 *
 * The wrapper registers every table of the module at construction, so a single instance is
 * enough to run queries and schema operations.
 *
 * @param jdbcUrl JDBC URL of the database, overridable through the `JDBC_URL` environment
 * variable.
 * @param driverClassName JDBC driver class name, overridable through the `JDBC_DRIVER`
 * environment variable.
 * @see [DatabaseWrapper]
 */
class DatabaseManager(
    jdbcUrl: String = System.getenv("JDBC_URL") ?: "jdbc:h2:mem:shikkanime;DB_CLOSE_DELAY=-1",
    driverClassName: String = System.getenv("JDBC_DRIVER") ?: "org.h2.Driver"
) {
    val databaseWrapper = DatabaseWrapper(
        jdbcUrl = jdbcUrl,
        driverClassName = driverClassName
    ).apply {
        addTables(
            UserTable,
            AnimeTable,
            AnimeLocalizationTable,
            AnimePlatformTable,
            AnimeMetadataLinkTable,
            ApiCacheTable,
            ImageTable,
            EpisodeTable,
            EpisodeLocalizationTable,
            EpisodeReleaseTable,
            EpisodeMetadataLinkTable,
            SimulcastTable,

            JoinAnimeSimulcastTable
        )
    }

    companion object {
        /**
         * Opens the file-backed database of the `data` directory and connects to it.
         *
         * The `data` folder is created when missing, so a fresh checkout provisions its database
         * on first run.
         *
         * @return the connected [DatabaseManager] instance.
         * @see [DatabaseManager]
         */
        fun connect(): DatabaseManager {
            val folder = File("data")
            if (!folder.exists())
                folder.mkdirs()
            val databaseManager = DatabaseManager("jdbc:h2:${folder.absolutePath}/database")
            databaseManager.databaseWrapper.connect()
            return databaseManager
        }
    }
}
