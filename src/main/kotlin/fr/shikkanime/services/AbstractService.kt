package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.repositories.AbstractRepository
import java.util.*

abstract class AbstractService<E : ShikkEntity, R : AbstractRepository<E>> {
    @Inject protected lateinit var repository: R

    open fun findAll() = repository.findAll()

    fun findAllUuids() = repository.findAllUuids()

    fun findAllByUuids(uuids: Collection<UUID>) = repository.findAllByUuids(uuids)

    fun getReference(uuid: UUID) = repository.getReference(uuid)

    fun find(uuid: UUID?) = if (uuid != null) repository.find(uuid) else null

    open fun save(entity: E) = repository.save(entity)

    open fun saveAll(entities: List<E>) = repository.saveAll(entities)

    open fun update(entity: E) = repository.update(entity)

    fun updateAll(entities: List<E>) = repository.updateAll(entities)

    open fun delete(entity: E) = repository.delete(entity)

    open fun deleteAll(entities: List<E>) = repository.deleteAll(entities)
}