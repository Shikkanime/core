package fr.shikkanime.database

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("fr.shikkanime.database")
class DatabaseModule

/**
 * Initializes the database schema of the application.
 *
 * Connects to the configured database and creates every table of the schema, so a fresh environment
 * is provisioned without an external migration step.
 *
 * @return nothing; the process exits once the schema is created.
 * @see [DatabaseManager]
 */
fun main() {
    DatabaseManager.connect()
        .databaseWrapper
        .initializeSchema()
}