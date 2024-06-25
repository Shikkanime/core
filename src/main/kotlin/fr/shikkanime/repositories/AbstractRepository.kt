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
        return database.entityManager.use {
            val transaction = it.transaction
            transaction.begin()
            val result: T

            try {
                result = block(it)
                transaction.commit()
            } catch (e: Exception) {
                transaction.rollback()
                throw e
            }

            result
        }
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
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            query.from(getEntityClass())
            createReadOnlyQuery(it, query).resultList
        }
    }

    open fun find(uuid: UUID): E? {
        return database.entityManager.use {
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