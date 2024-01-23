package fr.shikkanime.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.dtos.TokenDto
import fr.shikkanime.entities.enums.Role
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import java.util.*

fun Application.configureSecurity() {
    val memberService = Constant.injector.getInstance(MemberService::class.java)

    val jwtVerifier = JWT
        .require(Algorithm.HMAC256(Constant.jwtSecret))
        .withAudience(Constant.jwtAudience)
        .withIssuer(Constant.jwtDomain)
        .withClaimPresence("uuid")
        .withClaimPresence("username")
        .withClaimPresence("creationDateTime")
        .withClaimPresence("role")
        .build()

    authentication {
        jwt {
            realm = Constant.jwtRealm
            verifier(jwtVerifier)
            validate { credential ->
                if (credential.payload.audience.contains(Constant.jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }

        session<TokenDto>("auth-admin-session") {
            validate { session ->
                try {
                    val jwtPrincipal = jwtVerifier.verify(session.token)
                    val uuid = UUID.fromString(jwtPrincipal.getClaim("uuid").asString())
                    val username = jwtPrincipal.getClaim("username").asString()
                    val creationDateTime = jwtPrincipal.getClaim("creationDateTime").asString()
                    val role = Role.valueOf(jwtPrincipal.getClaim("role").asString())

                    val member = memberService.find(uuid) ?: return@validate null
                    if (member.username != username || member.role != role) return@validate null
                    if (member.creationDateTime.toString() != creationDateTime) return@validate null
                    if (member.role != Role.ADMIN) return@validate null
                    return@validate session
                } catch (e: Exception) {
                    println(e.message)
                    return@validate null
                }
            }
            challenge {
                // If content type is json, then respond with json
                if (call.request.contentType() == ContentType.Application.Json) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        MessageDto(MessageDto.Type.ERROR, "You are not authorized to access this page")
                    )
                } else {
                    call.respondRedirect("/admin?error=2", permanent = true)
                }
            }
        }
    }

    install(Sessions) {
        cookie<TokenDto>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 3600
        }
    }
}
