package fr.shikkanime.utils.entities

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Tracing(
    /**
     * Trace when a deleted is performed
     */
    val delete: Boolean = true
)
