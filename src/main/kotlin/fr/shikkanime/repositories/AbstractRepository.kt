package fr.shikkanime.repositories

import com.google.inject.Inject
import fr.shikkanime.utils.Database
import java.io.Serializable
import java.lang.reflect.ParameterizedType
import java.util.*

abstract class AbstractRepository<E : Serializable> {
    @Inject
    protected lateinit var database: Database

    protected fun getEntityClass(): Class<E> {
        return (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<E>
    }

    protected fun getEntityManager() = database.entityManager

    protected fun <T> inTransaction(block: () -> T): T {
        val transaction = getEntityManager().transaction
        transaction.begin()
        val result: T?

        try {
            result = block()
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        }

        return result
    }

    fun findAll(): List<E> {
        return getEntityManager().createQuery("FROM ${getEntityClass().simpleName}", getEntityClass()).resultList
    }

    fun find(uuid: UUID): E? {
        return getEntityManager().find(getEntityClass(), uuid)
    }

    fun save(entity: E): E {
        return inTransaction {
            getEntityManager().persist(entity)
            entity
        }
    }

    fun update(entity: E): E {
        return inTransaction {
            getEntityManager().merge(entity)
            entity
        }
    }

    fun delete(entity: E) {
        inTransaction {
            getEntityManager().remove(entity)
        }
    }
}