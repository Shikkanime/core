package fr.shikkanime.utils.routes

import fr.shikkanime.dtos.TokenDto
import fr.shikkanime.entities.enums.Link
import io.ktor.http.*

enum class ResponseType {
    JSON,
    MULTIPART,
    TEMPLATE,
    REDIRECT,
}

open class Response(
    val status: HttpStatusCode = HttpStatusCode.OK,
    val type: ResponseType = ResponseType.JSON,
    val session: TokenDto? = null,
    val data: Any? = null,
    val contentType: ContentType = ContentType.Application.Json,
) {
    companion object {
        fun ok(data: Any? = null, session: TokenDto? = null): Response =
            Response(HttpStatusCode.OK, data = data, session = session)

        fun created(data: Any? = null, session: TokenDto? = null): Response =
            Response(HttpStatusCode.Created, data = data, session = session)

        fun noContent(data: Any? = null, session: TokenDto? = null): Response =
            Response(HttpStatusCode.NoContent, data = data, session = session)

        fun multipart(image: ByteArray, contentType: ContentType, session: TokenDto? = null): Response =
            Response(
                HttpStatusCode.OK,
                type = ResponseType.MULTIPART,
                data = mapOf("image" to image, "contentType" to contentType),
                session = session
            )

        fun template(
            code: HttpStatusCode,
            template: String,
            title: String? = null,
            model: Map<String, Any?> = mapOf(),
            session: TokenDto? = null,
            contentType: ContentType = ContentType.Text.Html.withCharset(Charsets.UTF_8),
        ): Response = Response(
            code,
            type = ResponseType.TEMPLATE,
            data = mapOf("template" to template, "title" to title, "model" to model),
            session = session,
            contentType = contentType
        )

        fun template(
            template: String,
            title: String? = null,
            model: Map<String, Any?> = mapOf(),
            session: TokenDto? = null,
            contentType: ContentType = ContentType.Text.Html.withCharset(Charsets.UTF_8),
        ) = template(HttpStatusCode.OK, template, title, model, session, contentType)

        fun template(
            link: Link,
            model: Map<String, Any?> = mapOf(),
            session: TokenDto? = null,
            contentType: ContentType = ContentType.Text.Html.withCharset(Charsets.UTF_8),
        ): Response = template(link.template, link.title, model, session, contentType)

        fun redirect(path: String, session: TokenDto? = null): Response =
            Response(HttpStatusCode.Found, type = ResponseType.REDIRECT, data = path, session = session)

        fun badRequest(data: Any? = null, session: TokenDto? = null): Response =
            Response(HttpStatusCode.BadRequest, data = data, session = session)

        fun notFound(data: Any? = null, session: TokenDto? = null): Response =
            Response(HttpStatusCode.NotFound, data = data, session = session)

        fun conflict(data: Any? = null, session: TokenDto? = null): Response =
            Response(HttpStatusCode.Conflict, data = data, session = session)
    }
}