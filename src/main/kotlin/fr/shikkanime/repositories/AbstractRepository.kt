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

    protected fun <T> inTransaction(block: () -> T): T {
        val transaction = database.entityManager.transaction
        transaction.begin()
        val result: T

        try {
            result = block()
            database.entityManager.flush()
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            database.entityManager.clear()
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
        val cb = database.entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        query.from(getEntityClass())

        return createReadOnlyQuery(database.entityManager, query)
            .resultList
    }

    open fun find(uuid: UUID): E? {
        return database.entityManager.find(getEntityClass(), uuid)
    }

    fun save(entity: E): E {
        return inTransaction {
            database.entityManager.persist(entity)
            entity
        }
    }

    fun update(entity: E): E {
        return inTransaction {
            database.entityManager.merge(entity)
            entity
        }
    }

    fun delete(entity: E) {
        inTransaction {
            database.entityManager.remove(entity)
        }
    }

    fun deleteAll() {
        inTransaction {
            database.entityManager.createQuery("DELETE FROM ${getEntityClass().simpleName}").executeUpdate()
        }
    }
}