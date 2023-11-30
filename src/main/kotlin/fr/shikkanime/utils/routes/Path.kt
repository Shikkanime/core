package fr.shikkanime.utils.routes

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Path(val value: String = "")
