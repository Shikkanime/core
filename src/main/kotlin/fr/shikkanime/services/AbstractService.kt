package fr.shikkanime.services

import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.repositories.AbstractRepository
import fr.shikkanime.utils.TelemetryConfig
import fr.shikkanime.utils.TelemetryConfig.span
import jakarta.persistence.EntityManager
import java.util.*

abstract class AbstractService<E : ShikkEntity, R : AbstractRepository<E>> {
    private val tracer = TelemetryConfig.getTracer("AbstractService")

    protected abstract fun getRepository(): R

    open fun findAll() = tracer.span { getRepository().findAll() }

    fun find(uuid: UUID?) = if (uuid != null) tracer.span { getRepository().find(uuid) } else null

    open fun save(entity: E) = tracer.span { getRepository().save(entity) }

    open fun update(entity: E) = tracer.span { getRepository().update(entity) }

    fun updateAll(entities: List<E>) = tracer.span { getRepository().updateAll(entities) }

    open fun delete(entity: E) = tracer.span { getRepository().delete(entity) }

    fun deleteAll(entityManager: EntityManager) = tracer.span { getRepository().deleteAll(entityManager) }
}