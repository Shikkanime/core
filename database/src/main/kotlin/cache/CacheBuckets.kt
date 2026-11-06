package fr.shikkanime.database.cache

/**
 * Cache buckets shared by the readers and the writers of the catalog.
 *
 * The suffix is the schema version of the cached payload: bump it when a DTO changes shape, so a
 * rolling deployment never decodes an entry written by the previous build. A writer invalidates a
 * bucket by incrementing its version, which makes every entry of the previous version unreachable.
 */
object CacheBuckets {
    const val ANIME = "anime:v1"
    const val EPISODE = "episode:v1"
}
