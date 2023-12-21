package fr.shikkanime.repositories

import com.google.inject.Inject
import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.utils.Database
import jakarta.persistence.EntityManager
import java.lang.reflect.ParameterizedType
import java.util.*

abstract class AbstractRepository<E : ShikkEntity> {
    @Inject
    protected lateinit var database: Database

    protected fun getEntityClass(): Class<E> {
        return (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<E>
    }

    protected fun <T> inTransaction(block: (EntityManager) -> T): T {
        val entityManager = database.getEntityManager()
        val transaction = entityManager.transaction
        transaction.begin()
        val result: T

        try {
            result = block(entityManager)
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            entityManager.close()
        }

        return result
    }

    open fun findAll(): List<E> {
        return inTransaction {
            it.createQuery("FROM ${getEntityClass().simpleName}", getEntityClass()).resultList
        }
    }

    open fun find(uuid: UUID): E? {
        return inTransaction {
            it.find(getEntityClass(), uuid)
        }
    }

    fun save(entity: E): E {
        return inTransaction {
            it.persist(entity)
            entity
        }
    }

    fun update(entity: E): E {
        return inTransaction {
            it.merge(entity)
            entity
        }
    }

    fun delete(entity: E) {
        inTransaction {
            it.remove(entity)
        }
    }
}