package fr.shikkanime.utils.routes

import fr.shikkanime.dtos.MemberDto
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
    val session: MemberDto? = null,
    val data: Any? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Response) return false

        if (status != other.status) return false
        if (data != other.data) return false
        if (session != other.session) return false

        return true
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + (data?.hashCode() ?: 0)
        result = 31 * result + (session?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Response(status=$status, data=$data, session=$session)"
    }

    companion object {
        fun ok(data: Any? = null, session: MemberDto? = null): Response = Response(HttpStatusCode.OK, data = data, session = session)
        fun created(data: Any? = null, session: MemberDto? = null): Response = Response(HttpStatusCode.Created, data = data, session = session)
        fun noContent(data: Any? = null, session: MemberDto? = null): Response = Response(HttpStatusCode.NoContent, data = data, session = session)
        fun multipart(image: ByteArray, contentType: ContentType, session: MemberDto? = null): Response =
            Response(HttpStatusCode.OK, type = ResponseType.MULTIPART, data = mapOf("image" to image, "contentType" to contentType), session = session)

        fun template(template: String, title: String? = null, model: MutableMap<String, Any> = mutableMapOf(), session: MemberDto? = null): Response = Response(
            HttpStatusCode.OK,
            type = ResponseType.TEMPLATE,
            data = mapOf("template" to template, "title" to title, "model" to model),
            session = session
        )

        fun redirect(path: String, session: MemberDto? = null): Response =
            Response(HttpStatusCode.Found, type = ResponseType.REDIRECT, data = path, session = session)

        fun badRequest(data: Any? = null, session: MemberDto? = null): Response = Response(HttpStatusCode.BadRequest, data = data, session = session)

        fun notFound(data: Any? = null, session: MemberDto? = null): Response = Response(HttpStatusCode.NotFound, data = data, session = session)
        fun conflict(data: Any? = null, session: MemberDto? = null): Response = Response(HttpStatusCode.Conflict, data = data, session = session)
    }
}