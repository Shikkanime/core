package fr.shikkanime.utils

import fr.shikkanime.entities.ShikkEntity
import jakarta.persistence.EntityManager
import liquibase.command.CommandScope
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import org.hibernate.internal.SessionFactoryImpl
import java.io.File
import java.util.logging.Level
import kotlin.system.exitProcess

class Database {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val sessionFactory: SessionFactory

    constructor(file: File) {
        if (!file.exists()) {
            throw Exception("File ${file.absolutePath} does not exist")
        }

        val configuration = Configuration()
        val entities = Constant.reflections.getSubTypesOf(ShikkEntity::class.java)
        entities.forEach { configuration.addAnnotatedClass(it) }
        configuration.configure(file)

        val databaseUrl: String? = System.getenv("DATABASE_URL")
        val databaseUsername: String? = System.getenv("DATABASE_USERNAME")
        val databasePassword: String? = System.getenv("DATABASE_PASSWORD")

        if (databaseUrl?.isNotBlank() == true) {
            configuration.setProperty("hibernate.connection.url", databaseUrl)
            logger.config("Bypassing hibernate.cfg.xml with system environment variable DATABASE_URL")
        }

        if (databaseUsername?.isNotBlank() == true) {
            configuration.setProperty("hibernate.connection.username", databaseUsername)
            logger.config("Bypassing hibernate.cfg.xml with system environment variable DATABASE_USERNAME")
        }

        if (databasePassword?.isNotBlank() == true) {
            configuration.setProperty("hibernate.connection.password", databasePassword)
            logger.config("Bypassing hibernate.cfg.xml with system environment variable DATABASE_PASSWORD")
        }

        try {
            sessionFactory = configuration.buildSessionFactory()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while building session factory", e)
            exitProcess(1)
        }

        executeLiquibaseChangelogs(configuration)
    }

    private fun executeLiquibaseChangelogs(configuration: Configuration) {
        sessionFactory.openSession().use { session ->
            session.doWork {
                try {
                    CommandScope("update")
                        .addArgumentValue("changeLogFile", "db/changelog/db.changelog-master.xml")
                        .addArgumentValue("url", configuration.getProperty("hibernate.connection.url"))
                        .addArgumentValue("username", configuration.getProperty("hibernate.connection.username"))
                        .addArgumentValue("password", configuration.getProperty("hibernate.connection.password"))
                        .execute()
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error while updating database", e)
                    exitProcess(1)
                }
            }
        }

        try {
            sessionFactory.schemaManager.validateMappedObjects()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while validating database", e)
            exitProcess(1)
        }
    }

    constructor() : this(
        File(
            ClassLoader.getSystemClassLoader().getResource("hibernate.cfg.xml")?.file ?: "hibernate.cfg.xml"
        )
    )

    val entityManager: EntityManager
        get() = sessionFactory.createEntityManager()

    fun <T> inTransaction(block: (EntityManager) -> T): T = sessionFactory.callInTransaction(block)

    fun dialect(): String = (sessionFactory as SessionFactoryImpl).jdbcServices.dialect.toString()

    fun truncate() = sessionFactory.schemaManager.truncate()

    fun clearCache() = sessionFactory.cache.evictAllRegions()
}