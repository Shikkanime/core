package fr.shikkanime.utils.routes

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class JWTAuthenticated(val optional: Boolean = false)
