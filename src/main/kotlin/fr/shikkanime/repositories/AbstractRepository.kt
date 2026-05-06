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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hibernate.ScrollMode
import org.hibernate.jpa.AvailableHints
import org.hibernate.query.Query
import java.lang.reflect.ParameterizedType
import java.util.*

abstract class AbstractRepository<E : ShikkEntity> {
    @Inject protected lateinit var database: Database

    @Suppress("UNCHECKED_CAST")
    protected open fun getEntityClass() =
        (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<E>

    private suspend inline fun <T> dispatchWithContext(crossinline block: suspend () -> T): T =
        withContext(Dispatchers.IO) { block() }

    suspend fun <T> dispatch(transaction: Boolean = false, block: (EntityManager) -> T): T =
        dispatchWithContext {
            if (transaction) database.inTransaction(block)
            else database.entityManager.use(block)
        }

    suspend fun <T> dispatchSuspending(block: suspend (EntityManager) -> T): T =
        dispatchWithContext {
            database.entityManager.use { entityManager ->
                block(entityManager)
            }
        }

    fun <T> createReadOnlyQuery(entityManager: EntityManager, query: CriteriaQuery<T>) = createReadOnlyQuery(entityManager.createQuery(query))

    fun <T> createReadOnlyQuery(query: TypedQuery<T>): TypedQuery<T> {
        return query.setHint(AvailableHints.HINT_READ_ONLY, true)
            .setHint(AvailableHints.HINT_CACHEABLE, true)
    }

    suspend inline fun <reified C> buildPageableQuery(query: TypedQuery<C>, page: Int, limit: Int): Pageable<C> {
        val list = mutableSetOf<C>()
        var total = 0L

        dispatch {
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
        }

        return Pageable(list, page, limit, total)
    }

    open suspend fun findAll(): List<E> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            query.from(getEntityClass())
            createReadOnlyQuery(it, query).resultList
        }
    }

    suspend fun findAllUuids(): List<UUID> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(UUID::class.java)
            val root = query.from(getEntityClass())
            query.select(root[ShikkEntity_.uuid])
            createReadOnlyQuery(it, query).resultList
        }
    }

    suspend fun findAllByUuids(uuids: Collection<UUID>): List<E> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.where(root[ShikkEntity_.uuid].`in`(uuids))
            createReadOnlyQuery(it, query).resultList
        }
    }

    suspend fun getReference(uuid: UUID): E? {
        return dispatch {
            try {
                it.getReference(getEntityClass(), uuid)
            } catch (_: EntityNotFoundException) {
                null
            }
        }
    }

    open suspend fun find(uuid: UUID): E? {
        return dispatch {
            it.find(getEntityClass(), uuid)
        }
    }

    suspend fun save(entity: E): E {
        return dispatch(true) {
            it.persist(entity)
            entity
        }
    }

    suspend fun saveAll(entities: List<E>) {
        return dispatch(true) { entityManager ->
            entities.forEach {
                entityManager.persist(it)
            }
        }
    }

    suspend fun update(entity: E): E {
        return dispatch(true) {
            it.merge(entity)
        }
    }

    suspend fun updateAll(entities: List<E>) {
        return dispatch(true) { entityManager ->
            entities.forEach {
                entityManager.merge(it)
            }
        }
    }

    suspend fun delete(entity: E) {
        dispatch(true) {
            it.remove(entity)
        }
    }

    suspend fun deleteAll(entities: List<E>) {
        dispatch(true) { entityManager ->
            entities.forEach {
                entityManager.remove(it)
            }
        }
    }
}