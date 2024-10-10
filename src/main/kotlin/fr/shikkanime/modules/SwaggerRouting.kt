package fr.shikkanime.modules

import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.github.smiley4.ktorswaggerui.data.ArrayTypeDescriptor
import io.github.smiley4.ktorswaggerui.data.KTypeDescriptor
import io.github.smiley4.ktorswaggerui.data.ValueExampleDescriptor
import io.github.smiley4.ktorswaggerui.dsl.routes.OpenApiRoute
import io.github.smiley4.ktorswaggerui.dsl.routes.OpenApiSimpleBody
import io.ktor.http.*
import io.ktor.http.content.*
import java.io.File
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.starProjectedType
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
        securitySchemeNames = if (openApi.security) listOf("BearerAuth") else null
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
                        val typeDescriptor =
                            if (type.java.isArray) ArrayTypeDescriptor(KTypeDescriptor(type.java.componentType.kotlin.starProjectedType)) else KTypeDescriptor(
                                type.starProjectedType
                            )

                        queryParameter(qp.name.ifBlank { name }, typeDescriptor) {
                            description = qp.description
                            required = qp.required
                            example = ValueExampleDescriptor("", qp.example.takeIf { it.isNotBlank() })
                        }
                    }

                    parameter.hasAnnotation<PathParam>() -> {
                        val pp = parameter.findAnnotation<PathParam>()!!
                        pathParameter(pp.name.ifBlank { name }, type.starProjectedType) {
                            description = pp.description
                            required = true
                        }
                    }

                    parameter.hasAnnotation<BodyParam>() -> {
                        if (parameter.type.javaType == MultiPartData::class.java) {
                            multipartBody {
                                description = "Multipart data"
                                mediaTypes = listOf(ContentType.MultiPart.FormData)
                                part<File>("file") {
                                    mediaTypes = listOf(ContentType.Image.PNG, ContentType.Image.JPEG)
                                }
                            }
                        } else {
                            body(type.starProjectedType)
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
                val block: OpenApiSimpleBody.() -> Unit = { mediaTypes(ContentType.parse(response.contentType)) }
                val typeDescriptor =
                    if (response.type.java.isArray) ArrayTypeDescriptor(KTypeDescriptor(response.type.java.componentType.kotlin.starProjectedType)) else KTypeDescriptor(
                        response.type.starProjectedType
                    )
                body(typeDescriptor, block)
            }
        }
    }
}