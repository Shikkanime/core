package fr.shikkanime.modules

import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.github.smiley4.ktorswaggerui.dsl.BodyTypeDescriptor
import io.github.smiley4.ktorswaggerui.dsl.OpenApiRoute
import io.github.smiley4.ktorswaggerui.dsl.OpenApiSimpleBody
import io.ktor.http.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.jvmErasure

fun swagger(
    method: KFunction<*>,
    routeTags: List<String>,
    hiddenRoute: Boolean
): OpenApiRoute.() -> Unit {
    val openApi = method.findAnnotation<OpenAPI>() ?: return {
        tags = routeTags
        hidden = hiddenRoute
    }

    return {
        tags = routeTags
        hidden = hiddenRoute || openApi.hidden
        description = openApi.description
        swaggerRequest(method)
        swaggerResponse(openApi)
    }
}

private fun OpenApiRoute.swaggerRequest(method: KFunction<*>) {
    request {
        method.parameters.filter { it.hasAnnotation<QueryParam>() || it.hasAnnotation<PathParam>() }
            .forEach { parameter ->
                val name = parameter.name!!
                val type = parameter.type.jvmErasure

                when {
                    parameter.hasAnnotation<QueryParam>() -> {
                        val qp = parameter.findAnnotation<QueryParam>()!!
                        queryParameter(qp.name.ifBlank { name }, type) {
                            description = qp.description
                            required = qp.required
                        }
                    }

                    parameter.hasAnnotation<PathParam>() -> {
                        val pp = parameter.findAnnotation<PathParam>()!!
                        pathParameter(pp.name.ifBlank { name }, type) {
                            description = pp.description
                            required = true
                        }
                    }
                }
            }
    }
}

private fun OpenApiRoute.swaggerResponse(openApi: OpenAPI) {
    response {
        openApi.responses.forEach { response ->
            HttpStatusCode.fromValue(response.status) to {
                description = response.description
                val block: OpenApiSimpleBody.() -> Unit = { mediaType(ContentType.parse(response.contentType)) }

                when {
                    response.type.java.isArray -> body(
                        BodyTypeDescriptor.multipleOf(response.type.java.componentType.kotlin),
                        block
                    )

                    else -> body(response.type, block)
                }
            }
        }
    }
}