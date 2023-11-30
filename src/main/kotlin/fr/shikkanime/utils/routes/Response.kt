package fr.shikkanime.utils.routes

import io.ktor.http.*

open class Response(
    val status: HttpStatusCode = HttpStatusCode.OK,
    val data: Any? = null,
) {
    companion object {
        fun ok(data: Any? = null): Response = Response(HttpStatusCode.OK, data)
        fun created(data: Any?): Response = Response(HttpStatusCode.Created, data)
        fun noContent(): Response = Response(HttpStatusCode.NoContent)
    }
}

open class MultipartResponse(
    val image: ByteArray,
    val contentType: ContentType,
) : Response()

open class TemplateResponse(
    val template: String,
    val title: String? = null,
    val model: MutableMap<String, Any> = mutableMapOf(),
) : Response()

open class RedirectResponse(
    val path: String,
) : Response(HttpStatusCode.Found)