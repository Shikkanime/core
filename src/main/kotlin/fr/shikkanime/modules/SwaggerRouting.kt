package fr.shikkanime.modules

import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.github.smiley4.ktorswaggerui.dsl.BodyTypeDescriptor
import io.github.smiley4.ktorswaggerui.dsl.OpenApiRoute
import io.github.smiley4.ktorswaggerui.dsl.OpenApiSimpleBody
import io.ktor.http.*
import io.ktor.http.content.*
import java.io.File
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.javaType
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
        securitySchemeName = if (openApi.security) "BearerAuth" else null
        tags = routeTags
        hidden = hiddenRoute || openApi.hidden
        summary = openApi.description
        description = openApi.description
        deprecated = openApi.deprecated
        swaggerRequest(method)
        swaggerResponse(openApi)
    }
}

private fun OpenApiRoute.swaggerRequest(method: KFunction<*>) {
    request {
        method.parameters.filter { it.hasAnnotation<QueryParam>() || it.hasAnnotation<PathParam>() || it.hasAnnotation<BodyParam>() }
            .forEach { parameter ->
                val name = parameter.name!!
                val type = parameter.type.jvmErasure

                when {
                    parameter.hasAnnotation<QueryParam>() -> {
                        val qp = parameter.findAnnotation<QueryParam>()!!
                        queryParameter(qp.name.ifBlank { name }, type) {
                            description = qp.description
                            required = qp.required
                            example = qp.example.takeIf { it.isNotBlank() }
                        }
                    }

                    parameter.hasAnnotation<PathParam>() -> {
                        val pp = parameter.findAnnotation<PathParam>()!!
                        pathParameter(pp.name.ifBlank { name }, type) {
                            description = pp.description
                            required = true
                        }
                    }

                    parameter.hasAnnotation<BodyParam>() -> {
                        if (parameter.type.javaType == MultiPartData::class.java) {
                            multipartBody {
                                description = "Multipart data"
                                mediaType(ContentType.MultiPart.FormData)
                                part<File>("file") {
                                    mediaTypes = listOf(ContentType.Image.PNG, ContentType.Image.JPEG)
                                }
                            }
                        } else {
                            body(type)
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