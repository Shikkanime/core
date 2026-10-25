package fr.shikkanime.database

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("fr.shikkanime.database")
class DatabaseModule

/**
 * Provisions the database schema of the application.
 *
 * Runs the Liquibase changelog named by the `LIQUIBASE_CHANGELOG` environment variable when it is
 * set, and creates the missing tables otherwise: a fresh environment is provisioned without an
 * external migration step, and an existing one keeps its data. Every application image runs this
 * before starting its module.
 *
 * @return nothing; the process exits once the schema is up to date.
 * @see [DatabaseManager]
 */
fun main() {
    val databaseManager = DatabaseManager.connect()
    val changelog = System.getenv("LIQUIBASE_CHANGELOG")

    if (changelog.isNullOrBlank())
        databaseManager.databaseWrapper.initializeSchema()
    else
        databaseManager.databaseWrapper.migrateSchema(changelog)
}