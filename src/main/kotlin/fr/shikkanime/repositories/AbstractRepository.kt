package fr.shikkanime.repositories

import com.google.inject.Inject
import fr.shikkanime.entities.Pageable
import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.utils.Database
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import org.hibernate.ScrollMode
import org.hibernate.jpa.AvailableHints
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
        val entityManager = database.entityManager
        val transaction = entityManager.transaction
        if (!transaction.isActive) transaction.begin()
        val result: T

        try {
            result = block(entityManager)
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        }

        return result
    }

    fun <T> createQuery(query: String, clazz: Class<T>): TypedQuery<T> {
        return database.entityManager.createQuery(query, clazz)
            .setHint(AvailableHints.HINT_READ_ONLY, true)
    }

    fun buildPageableQuery(
        query: TypedQuery<E>,
        page: Int,
        limit: Int
    ): Pageable<E> {
        val scrollableResults = query.unwrap(Query::class.java)
            .setReadOnly(true)
            .setFetchSize(limit)
            .scroll(ScrollMode.SCROLL_SENSITIVE)

        val list = mutableListOf<E>()
        var total = 0L

        scrollableResults.use {
            if (!it.first())
                return@use

            val result = it.scroll((limit * page) - limit)

            if (!result)
                return@use

            for (i in 0 until limit) {
                @Suppress("UNCHECKED_CAST")
                list.add(it.get() as E)

                if (!it.next())
                    break
            }

            total = if (it.last()) it.rowNumber + 1L else 0
        }

        return Pageable(list, page, limit, total)
    }

    open fun findAll(): List<E> {
        return createQuery("FROM ${getEntityClass().simpleName}", getEntityClass())
            .resultList
    }

    open fun find(uuid: UUID): E? {
        return createQuery("FROM ${getEntityClass().simpleName} WHERE uuid = :uuid", getEntityClass())
            .setParameter("uuid", uuid)
            .resultList
            .firstOrNull()
    }

    fun save(entity: E): E {
        return inTransaction {
            it.persist(entity)
            it.flush()
            entity
        }
    }

    fun update(entity: E): E {
        return inTransaction {
            it.merge(entity)
            it.flush()
            entity
        }
    }

    fun delete(entity: E) {
        inTransaction {
            it.remove(entity)
            it.flush()
        }
    }

    fun deleteAll() {
        inTransaction {
            it.createQuery("DELETE FROM ${getEntityClass().simpleName}").executeUpdate()
        }
    }
}