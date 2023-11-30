package fr.shikkanime.utils

import fr.shikkanime.entities.ShikkEntity
import jakarta.persistence.EntityManager
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.hibernate.cfg.Configuration
import java.io.File
import kotlin.system.exitProcess

class Database {
    val entityManager: EntityManager

    constructor(file: File) {
        if (!file.exists()) {
            throw Exception("File ${file.absolutePath} does not exist")
        }

        val configuration = Configuration()
        val entities = Constant.reflections.getSubTypesOf(ShikkEntity::class.java)
        entities.forEach { configuration.addAnnotatedClass(it) }
        configuration.configure(file)
        val buildSessionFactory = configuration.buildSessionFactory()
        entityManager = buildSessionFactory.createEntityManager()

        buildSessionFactory.openSession().doWork {
            val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(it))
            val liquibase = Liquibase("db/changelog/db.changelog-master.xml", ClassLoaderResourceAccessor(), database)

            try {
                liquibase.update(Contexts(), LabelExpression())
            } catch (e: Exception) {
                e.printStackTrace()
                exitProcess(1)
            }
        }

        try {
            buildSessionFactory.schemaManager.validateMappedObjects()
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(1)
        }
    }

    constructor() : this(File("hibernate.cfg.xml"))

    fun getSize(): Long {
        return entityManager.createNativeQuery("SELECT pg_database_size('shikkanime')").singleResult as Long
    }
}