package fr.shikkanime.database

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("fr.shikkanime.database")
class DatabaseModule

fun main() {
    DatabaseManager.connect()
        .databaseWrapper
        .initializeSchema()
}