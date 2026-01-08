package fr.shikkanime.services

import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.repositories.AbstractRepository
import java.util.*

abstract class AbstractService<E : ShikkEntity, R : AbstractRepository<E>> {
    protected abstract fun getRepository(): R

    open fun findAll() = getRepository().findAll()

    fun findAllUuids() = getRepository().findAllUuids()

    fun findAllByUuids(uuids: Collection<UUID>) = getRepository().findAllByUuids(uuids)

    fun getReference(uuid: UUID) = getRepository().getReference(uuid)

    fun find(uuid: UUID?) = if (uuid != null) getRepository().find(uuid) else null

    open fun save(entity: E) = getRepository().save(entity)

    open fun saveAll(entities: List<E>) = getRepository().saveAll(entities)

    open fun update(entity: E) = getRepository().update(entity)

    fun updateAll(entities: List<E>) = getRepository().updateAll(entities)

    open fun delete(entity: E) = getRepository().delete(entity)

    open fun deleteAll(entities: List<E>) = getRepository().deleteAll(entities)
}