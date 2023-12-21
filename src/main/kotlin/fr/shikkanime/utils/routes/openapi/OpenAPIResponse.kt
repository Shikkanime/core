package fr.shikkanime.utils.routes.openapi

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class OpenAPIResponse(
    val status: Int,
    val description: String = "",
    val type: KClass<*> = Unit::class,
    val contentType: String = "application/json"
)
