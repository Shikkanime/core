package fr.shikkanime.database.repositories

import fr.shikkanime.exposed.repositories.EntityRepository
import fr.shikkanime.models.ModelConvertible
import fr.shikkanime.models.ShikkModel
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

/**
 * Base repository exposing domain models on top of the framework entity reads.
 *
 * It extends the framework [EntityRepository], so implementations keep the whole framework API
 * (`findById`, `findAllByIds`, and the worker-style upsert chains `ifExists`, `newIfNotExists`,
 * `applyFlush`). Models are mapped by the entities themselves, through [ModelConvertible].
 *
 * The mapped read is named `findModelById` and not `findById` because [EntityRepository] already
 * declares the entity-level `findById`: the `Model` keyword only appears where a parent repository
 * method would otherwise be hidden.
 *
 * @param T type of the entity identifier.
 * @param ENTITY type of Exposed entity managed by this repository.
 * @param MODEL type of domain model returned by this repository.
 * @param entityClass Exposed DAO class used to create and query entities.
 */
abstract class ModelRepository<T : Any, ENTITY, MODEL : ShikkModel>(
    entityClass: EntityClass<T, ENTITY>
) : EntityRepository<T, ENTITY>(entityClass)
    where ENTITY : Entity<T>, ENTITY : ModelConvertible<MODEL> {
    /**
     * Finds a domain model by its identifier.
     *
     * @param id identifier to search for.
     * @return the matching model, or `null` when it does not exist.
     */
    open fun findModelById(id: T): MODEL? =
        findById(id)?.toModel()
}
