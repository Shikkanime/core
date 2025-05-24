package fr.shikkanime.utils.routes.param

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class QueryParam(
    val name: String = "",
    val defaultValue: String = "",
)
