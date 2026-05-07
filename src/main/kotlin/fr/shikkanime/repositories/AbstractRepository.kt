package fr.shikkanime.repositories

import com.google.inject.Inject
import com.google.inject.Provider
import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.entities.ShikkEntity_
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.services.TraceActionService
import fr.shikkanime.utils.Database
import fr.shikkanime.utils.entities.Tracing
import fr.shikkanime.utils.onTrue
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
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.full.findAnnotation

private class EntityManagerContext(val entityManager: EntityManager) : AbstractCoroutineContextElement(EntityManagerContext) {
    companion object Key : CoroutineContext.Key<EntityManagerContext>
}

abstract class AbstractRepository<E : ShikkEntity> {
    @Inject protected lateinit var database: Database
    @Inject private lateinit var traceActionServiceProvider: Provider<TraceActionService>

    @Suppress("UNCHECKED_CAST")
    protected val entityClass by lazy { (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<E> }
    private val tracingAnnotation by lazy { entityClass.kotlin.findAnnotation<Tracing>() }

    private fun shouldTraceAction(delete: Boolean = false): Boolean {
        val tracing = tracingAnnotation ?: return false
        return !(delete && !tracing.delete)
    }

    suspend fun <T> dispatch(transaction: Boolean = false, block: suspend (EntityManager) -> T): T =
        withContext(Dispatchers.IO) {
            val entityManager = coroutineContext[EntityManagerContext]?.entityManager
            val execute: suspend (EntityManager) -> T = { entityManager ->
                val transaction = transaction.onTrue { entityManager.transaction }
                val shouldManageTransaction = transaction != null && !transaction.isActive

                try {
                    if (shouldManageTransaction) transaction.begin()
                    val result = block(entityManager)
                    if (shouldManageTransaction) transaction.commit()
                    result
                } catch (e: Exception) {
                    if (shouldManageTransaction && transaction.isActive) transaction.rollback()
                    throw e
                }
        }

            if (entityManager != null)
                return@withContext execute(entityManager)

            database.entityManager.use { entityManager ->
                withContext(EntityManagerContext(entityManager)) {
                    execute(entityManager)
                }
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
            val query = cb.createQuery(entityClass)
            query.from(entityClass)
            createReadOnlyQuery(it, query).resultList
        }
    }

    suspend fun findAllUuids(): List<UUID> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(UUID::class.java)
            val root = query.from(entityClass)
            query.select(root[ShikkEntity_.uuid])
            createReadOnlyQuery(it, query).resultList
        }
    }

    suspend fun findAllByUuids(uuids: Collection<UUID>): List<E> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)
            query.where(root[ShikkEntity_.uuid].`in`(uuids))
            createReadOnlyQuery(it, query).resultList
        }
    }

    suspend fun getReference(uuid: UUID): E? {
        return dispatch {
            try {
                it.getReference(entityClass, uuid)
            } catch (_: EntityNotFoundException) {
                null
            }
        }
    }

    open suspend fun find(uuid: UUID): E? {
        return dispatch {
            it.find(entityClass, uuid)
        }
    }

    suspend fun save(entity: E): E {
        return dispatch(true) {
            it.persist(entity)
            if (shouldTraceAction())
                traceActionServiceProvider.get().createTraceAction(entity, TraceAction.Action.CREATE)
            entity
        }
    }

    suspend fun saveAll(entities: List<E>) {
        val shouldTraceAction = shouldTraceAction()

        return dispatch(true) { entityManager ->
            entities.forEach {
                entityManager.persist(it)
                if (shouldTraceAction)
                    traceActionServiceProvider.get().createTraceAction(it, TraceAction.Action.CREATE)
            }
        }
    }

    suspend fun update(entity: E): E {
        return dispatch(true) {
            it.merge(entity).apply {
                if (shouldTraceAction())
                    traceActionServiceProvider.get().createTraceAction(this, TraceAction.Action.UPDATE)
            }
        }
    }

    suspend fun updateAll(entities: List<E>) {
        val shouldTraceAction = shouldTraceAction()

        return dispatch(true) { entityManager ->
            entities.forEach {
                entityManager.merge(it)
                if (shouldTraceAction)
                    traceActionServiceProvider.get().createTraceAction(it, TraceAction.Action.UPDATE)
            }
        }
    }

    suspend fun delete(entity: E) {
        dispatch(true) {
            it.remove(entity)
            if (shouldTraceAction(true))
                traceActionServiceProvider.get().createTraceAction(entity, TraceAction.Action.DELETE)
        }
    }

    suspend fun deleteAll(entities: List<E>) {
        val shouldTraceAction = shouldTraceAction(true)

        dispatch(true) { entityManager ->
            entities.forEach {
                entityManager.remove(it)
                if (shouldTraceAction)
                    traceActionServiceProvider.get().createTraceAction(it, TraceAction.Action.DELETE)
            }
        }
    }
}