package fr.shikkanime.models

/**
 * Maps a persistence entity to its domain model.
 *
 * Implement it on every Exposed entity so [fr.shikkanime.models.ShikkModel] mapping is uniform and
 * `ModelRepository` can map entities without a repository-level override.
 *
 * @param MODEL domain model produced by the mapping.
 */
interface ModelConvertible<MODEL : ShikkModel> {
    /**
     * Maps this entity to its domain model.
     *
     * @return the domain model of this entity.
     */
    fun toModel(): MODEL
}
