package fr.shikkanime.repositories

import com.google.inject.Inject
import fr.shikkanime.entities.Pageable
import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.utils.Database
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import org.hibernate.query.Query
import java.lang.reflect.ParameterizedType
import java.util.*

abstract class AbstractRepository<E : ShikkEntity> {
    @Inject
    protected lateinit var database: Database

    protected fun getEntityClass(): Class<E> {
        @Suppress("UNCHECKED_CAST")
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

    fun buildPageableQuery(
        query: TypedQuery<E>,
        page: Int,
        limit: Int
    ): Pageable<E> {
        val scrollableResults = (query as Query).scroll()
        scrollableResults.last()
        val total = scrollableResults.rowNumber + 1
        scrollableResults.close()

        query.setFirstResult((limit * page) - limit).setMaxResults(limit)
        return Pageable(query.resultList, page, limit, total.toLong())
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

    fun deleteAll() {
        inTransaction {
            it.createQuery("DELETE FROM ${getEntityClass().simpleName}").executeUpdate()
        }
    }
}