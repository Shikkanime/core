package fr.shikkanime.services

import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.repositories.AbstractRepository
import java.util.*

abstract class AbstractService<E : ShikkEntity, R : AbstractRepository<E>> {
    protected abstract fun getRepository(): R

    open fun findAll() = getRepository().findAll()

    fun find(uuid: UUID?) = if (uuid != null) getRepository().find(uuid) else null

    open fun save(entity: E) = getRepository().save(entity)

    open fun update(entity: E) = getRepository().update(entity)

    open fun delete(entity: E) = getRepository().delete(entity)

    fun deleteAll() = getRepository().deleteAll()
}