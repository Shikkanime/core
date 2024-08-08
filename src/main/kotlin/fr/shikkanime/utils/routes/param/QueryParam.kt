package fr.shikkanime.utils.routes.param

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class QueryParam(
    val name: String = "",
    val type: KClass<*> = Unit::class,
    val description: String = "",
    val required: Boolean = false,
    val example: String = "",
)
