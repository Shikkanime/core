package fr.shikkanime.utils.routes.openapi

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class OpenAPI(
    val description: String = "",
    val responses: Array<OpenAPIResponse> = [],
    val hidden: Boolean = false,
    val security: Boolean = false,
    val deprecated: Boolean = false
)
