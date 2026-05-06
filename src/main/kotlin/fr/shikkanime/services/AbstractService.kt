package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.repositories.AbstractRepository
import java.util.*

abstract class AbstractService<E : ShikkEntity, R : AbstractRepository<E>> {
    @Inject protected lateinit var repository: R

    open suspend fun findAll() = repository.findAll()

    suspend fun findAllUuids() = repository.findAllUuids()

    suspend fun findAllByUuids(uuids: Collection<UUID>) = repository.findAllByUuids(uuids)

    suspend fun getReference(uuid: UUID) = repository.getReference(uuid)

    suspend fun find(uuid: UUID?) = uuid?.let { repository.find(it) }

    open suspend fun save(entity: E) = repository.save(entity)

    open suspend fun saveAll(entities: List<E>) = repository.saveAll(entities)

    open suspend fun update(entity: E) = repository.update(entity)

    suspend fun updateAll(entities: List<E>) = repository.updateAll(entities)

    open suspend fun delete(entity: E) = repository.delete(entity)

    open suspend fun deleteAll(entities: List<E>) = repository.deleteAll(entities)
}