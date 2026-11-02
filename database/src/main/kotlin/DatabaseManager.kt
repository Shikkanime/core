package fr.shikkanime.database

import fr.shikkanime.database.entities.AnimeLocalizationTable
import fr.shikkanime.database.entities.AnimePlatformTable
import fr.shikkanime.database.entities.AnimeTable
import fr.shikkanime.database.entities.ImagePayloadTable
import fr.shikkanime.database.entities.ImageTable
import fr.shikkanime.database.entities.JoinAnimeSimulcastTable
import fr.shikkanime.database.entities.SimulcastTable
import fr.shikkanime.database.entities.UserTable
import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.exposed.DatabaseWrapper
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File

class DatabaseManager(
    jdbcUrl: String = System.getenv("JDBC_URL") ?: "jdbc:h2:mem:shikkanime;DB_CLOSE_DELAY=-1",
    driverClassName: String = System.getenv("JDBC_DRIVER") ?: "org.h2.Driver"
) {
    private val logger = LoggerFactory.getLogger()

    val databaseWrapper = DatabaseWrapper(
        jdbcUrl = jdbcUrl,
        driverClassName = driverClassName
    ).apply {
        addTables(*TABLES.toTypedArray())
    }

    /**
     * Brings an existing schema up to date.
     *
     * The framework provisioner only creates the tables it does not find: a column added to an
     * existing table would never reach a database created by a previous version. This converges the
     * columns as well, which is what a deployment to another machine needs.
     *
     * Exposed deprecates this call in favour of a migration tool: the project migrates through the
     * Liquibase changelog when one is configured, and this convergence only serves the development
     * databases that have none. It is a stop-gap until the changelog covers the image columns.
     *
     * @return nothing; the schema matches the table definitions.
     * @see [connect]
     */
    @Suppress("DEPRECATION")
    fun migrate() {
        // One table at a time: H2 cannot introspect a reserved table name, and that failure must not
        // stop the others from converging
        TABLES.forEach { table ->
            runCatching { transaction { SchemaUtils.createMissingTablesAndColumns(table) } }
                .onFailure { logger.warning("Could not converge ${table.tableName}: ${it.message}") }
        }
    }

    companion object {
        /** Every table of the schema, registered for creation and convergence. */
        val TABLES = listOf(
            UserTable,
            AnimeTable,
            AnimeLocalizationTable,
            AnimePlatformTable,
            ImageTable,
            ImagePayloadTable,
            SimulcastTable,
            JoinAnimeSimulcastTable
        )

        fun connect(): DatabaseManager {
            val folder = File("data")
            if (!folder.exists()) folder.mkdirs()
            val databaseManager = DatabaseManager("jdbc:h2:${folder.absolutePath}/database")
            databaseManager.databaseWrapper.connect()
            return databaseManager
        }
    }
}
