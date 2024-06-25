package fr.shikkanime.repositories

import com.google.inject.Inject
import fr.shikkanime.entities.Pageable
import fr.shikkanime.entities.ShikkEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaQuery
import org.hibernate.ScrollMode
import org.hibernate.jpa.AvailableHints
import org.hibernate.query.Query
import java.util.*

abstract class AbstractRepository<E : ShikkEntity> {
    @Inject
    @PersistenceContext
    protected lateinit var entityManager: EntityManager

    protected abstract fun getEntityClass(): Class<E>

    protected fun <T> inTransaction(block: () -> T): T {
        val transaction = entityManager.transaction
        transaction.begin()
        val result: T

        try {
            result = block()
            entityManager.flush()
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            entityManager.clear()
        }

        return result
    }

    fun <T> createReadOnlyQuery(entityManager: EntityManager, criteriaQuery: CriteriaQuery<T>): TypedQuery<T> {
        return entityManager.createQuery(criteriaQuery)
            .setHint(AvailableHints.HINT_READ_ONLY, true)
            .setHint(AvailableHints.HINT_CACHEABLE, true)
    }

    fun <C> buildPageableQuery(query: TypedQuery<C>, page: Int, limit: Int): Pageable<C> {
        val scrollableResults = query.unwrap(Query::class.java)
            .setReadOnly(true)
            .setFetchSize(limit)
            .scroll(ScrollMode.SCROLL_SENSITIVE)

        val list = mutableListOf<C>()
        var total = 0L

        if (scrollableResults.first() && scrollableResults.scroll((limit * page) - limit)) {
            for (i in 0 until limit) {
                list.add(scrollableResults.get() as C) // NOSONAR
                if (!scrollableResults.next()) break
            }
            total = if (scrollableResults.last()) scrollableResults.rowNumber + 1L else 0
        }

        return Pageable(list, page, limit, total)
    }

    open fun findAll(): List<E> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        query.from(getEntityClass())

        return createReadOnlyQuery(entityManager, query)
            .resultList
    }

    open fun find(uuid: UUID): E? {
        return entityManager.find(getEntityClass(), uuid)
    }

    fun save(entity: E): E {
        return inTransaction {
            entityManager.persist(entity)
            entity
        }
    }

    fun update(entity: E): E {
        return inTransaction {
            entityManager.merge(entity)
            entity
        }
    }

    fun delete(entity: E) {
        inTransaction {
            entityManager.remove(entity)
        }
    }

    fun deleteAll() {
        inTransaction {
            entityManager.createQuery("DELETE FROM ${getEntityClass().simpleName}").executeUpdate()
        }
    }
}