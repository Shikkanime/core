package fr.shikkanime.repositories

import com.google.inject.Inject
import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.entities.ShikkEntity_
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.utils.Database
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaQuery
import org.hibernate.ScrollMode
import org.hibernate.jpa.AvailableHints
import org.hibernate.query.Query
import java.util.*

abstract class AbstractRepository<E : ShikkEntity> {
    @Inject protected lateinit var database: Database

    protected abstract fun getEntityClass(): Class<E>

    fun <T> createReadOnlyQuery(entityManager: EntityManager, query: CriteriaQuery<T>) = createReadOnlyQuery(entityManager.createQuery(query))

    fun <T> createReadOnlyQuery(query: TypedQuery<T>): TypedQuery<T> {
        return query.setHint(AvailableHints.HINT_READ_ONLY, true)
            .setHint(AvailableHints.HINT_CACHEABLE, true)
    }

    inline fun <reified C> buildPageableQuery(query: TypedQuery<C>, page: Int, limit: Int): Pageable<C> {
        val list = mutableSetOf<C>()
        var total = 0L

        query.unwrap(Query::class.java)
            .setReadOnly(true)
            .setFetchSize(limit)
            .scroll(ScrollMode.SCROLL_SENSITIVE)
            .use { scrollableResults ->
                if (scrollableResults.first() && scrollableResults.scroll((limit * page) - limit)) {
                    (0 until limit).forEach { _ ->
                        val get = scrollableResults.get() as? C ?: return@forEach
                        list.add(get)
                        if (!scrollableResults.next()) return@forEach
                    }

                    total = if (scrollableResults.last()) scrollableResults.position.toLong() else 0L
                }
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

    fun findAllUuids(): List<UUID> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(UUID::class.java)
            val root = query.from(getEntityClass())
            query.select(root[ShikkEntity_.uuid])
            createReadOnlyQuery(it, query).resultList
        }
    }

    fun findAllByUuids(uuids: Collection<UUID>): List<E> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.where(root[ShikkEntity_.uuid].`in`(uuids))
            createReadOnlyQuery(it, query).resultList
        }
    }

    fun getReference(uuid: UUID): E? {
        return try {
            database.entityManager.use {
                it.getReference(getEntityClass(), uuid)
            }
        } catch (_: EntityNotFoundException) {
            null
        }
    }

    open fun find(uuid: UUID): E? {
        return database.entityManager.use {
            it.find(getEntityClass(), uuid)
        }
    }

    fun save(entity: E): E {
        return database.inTransaction {
            it.persist(entity)
            entity
        }
    }

    fun saveAll(entities: List<E>) {
        return database.inTransaction { entityManager ->
            entities.forEach { entityManager.persist(it) }
        }
    }

    fun update(entity: E): E {
        return database.inTransaction {
            it.merge(entity)
            entity
        }
    }

    fun updateAll(entities: List<E>) {
        return database.inTransaction { entityManager ->
            entities.forEach { entityManager.merge(it) }
        }
    }

    fun delete(entity: E) {
        database.inTransaction {
            it.remove(entity)
        }
    }
}