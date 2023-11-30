package fr.shikkanime.services

import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.repositories.AbstractRepository
import java.util.*

abstract class AbstractService<E : ShikkEntity, R : AbstractRepository<E>> {
    protected abstract fun getRepository(): R

    fun findAll() = getRepository().findAll()

    fun find(uuid: UUID) = getRepository().find(uuid)

    open fun save(entity: E) = getRepository().save(entity)

    fun update(entity: E) = getRepository().update(entity)

    fun delete(entity: E) = getRepository().delete(entity)
}