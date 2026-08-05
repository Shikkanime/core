package fr.shikkanime.database

import fr.shikkanime.database.entities.AnimeTable
import fr.shikkanime.database.entities.JoinAnimeSimulcastTable
import fr.shikkanime.database.entities.SimulcastTable
import fr.shikkanime.database.entities.UserTable
import fr.shikkanime.exposed.DatabaseWrapper
import java.io.File

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
            SimulcastTable,

            JoinAnimeSimulcastTable
        )
    }

    companion object {
        fun connect(): DatabaseManager {
            val folder = File("data")
            if (!folder.exists()) folder.mkdirs()
            val databaseManager = DatabaseManager("jdbc:h2:${folder.absolutePath}/database")
            databaseManager.databaseWrapper.connect()
            return databaseManager
        }
    }
}
