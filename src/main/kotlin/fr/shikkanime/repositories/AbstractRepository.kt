package fr.shikkanime.repositories

import com.google.inject.Inject
import fr.shikkanime.entities.Pageable
import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.utils.Database
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaQuery
import org.hibernate.ScrollMode
import org.hibernate.jpa.AvailableHints
import org.hibernate.query.Query
import java.util.*

abstract class AbstractRepository<E : ShikkEntity> {
    @Inject
    protected lateinit var database: Database

    protected abstract fun getEntityClass(): Class<E>

    protected fun <T> inTransaction(block: (EntityManager) -> T): T {
        val entityManager = database.entityManager
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

    fun <T> createReadOnlyQuery(entityManager: EntityManager, query: String, clazz: Class<T>): TypedQuery<T> {
        return entityManager.createQuery(query, clazz)
            .setHint(AvailableHints.HINT_READ_ONLY, true)
    }

    fun <T> createReadOnlyQuery(entityManager: EntityManager, criteriaQuery: CriteriaQuery<T>): TypedQuery<T> {
        return entityManager.createQuery(criteriaQuery)
            .setHint(AvailableHints.HINT_READ_ONLY, true)
    }

    fun buildPageableQuery(query: TypedQuery<E>, page: Int, limit: Int): Pageable<E> {
        val scrollableResults = query.unwrap(Query::class.java)
            .setReadOnly(true)
            .setFetchSize(limit)
            .scroll(ScrollMode.SCROLL_SENSITIVE)

        val list = mutableListOf<E>()
        var total = 0L

        if (scrollableResults.first() && scrollableResults.scroll((limit * page) - limit)) {
            for (i in 0 until limit) {
                list.add(scrollableResults.get() as E) // NOSONAR
                if (!scrollableResults.next()) break
            }
            total = if (scrollableResults.last()) scrollableResults.rowNumber + 1L else 0
        }

        return Pageable(list, page, limit, total)
    }

    open fun findAll(): List<E> {
        return inTransaction {
            createReadOnlyQuery(it, "FROM ${getEntityClass().simpleName}", getEntityClass())
                .resultList
        }
    }

    open fun find(uuid: UUID): E? {
        return inTransaction {
            createReadOnlyQuery(it, "FROM ${getEntityClass().simpleName} WHERE uuid = :uuid", getEntityClass())
                .setParameter("uuid", uuid)
                .resultList
                .firstOrNull()
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