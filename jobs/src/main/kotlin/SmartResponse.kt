package fr.shikkanime.jobs

/**
 * Payload of a fetch with the change detection of its content.
 *
 * This is the shared "did anything change" wrapper: the HTTP layer reports it for every cached
 * request, and a streaming platform reports it for a whole harvest.
 *
 * @param T type of the fetched data.
 * @param fetchedAtMillis the timestamp at which the payload was originally fetched, when known.
 */
data class SmartResponse<T>(
    val data: T,
    val hasChanged: Boolean,
    val fetchedAtMillis: Long? = null
)
