package fr.shikkanime.modules

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.dtos.TokenDto
import fr.shikkanime.entities.enums.Role
import fr.shikkanime.services.caches.MemberCacheService
import fr.shikkanime.utils.Constant
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import java.util.*

private val memberCacheService = Constant.injector.getInstance(MemberCacheService::class.java)

fun Application.configureSecurity() {
    val jwtVerifier = setupJWTVerifier()

    authentication {
        setupJWTAuthentication(jwtVerifier)
        setupSessionAuthentication(jwtVerifier)
    }

    install(Sessions) {
        cookie<TokenDto>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 3600
        }
    }
}

private fun setupJWTVerifier(): JWTVerifier = JWT
    .require(Algorithm.HMAC256(Constant.jwtSecret))
    .withAudience(Constant.jwtAudience)
    .withIssuer(Constant.jwtDomain)
    .withClaimPresence("uuid")
    .withClaimPresence("username")
    .withClaimPresence("creationDateTime")
    .withClaimPresence("roles")
    .build()

private fun AuthenticationConfig.setupJWTAuthentication(jwtVerifier: JWTVerifier) {
    jwt("auth-jwt") {
        realm = Constant.jwtRealm
        verifier(jwtVerifier)
        validate { credential ->
            if (credential.payload.audience.contains(Constant.jwtAudience)) JWTPrincipal(credential.payload) else null
        }
        challenge { _, _ ->
            call.respond(
                HttpStatusCode.Unauthorized,
                MessageDto(MessageDto.Type.ERROR, "You are not authorized to access this page")
            )
        }
    }
}

private fun AuthenticationConfig.setupSessionAuthentication(jwtVerifier: JWTVerifier) {
    session<TokenDto>("auth-admin-session") {
        validate { session -> validationSession(jwtVerifier, session) }
        challenge {
            if (call.request.contentType() != ContentType.Text.Html) {
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

private fun validationSession(jwtVerifier: JWTVerifier, session: TokenDto): TokenDto? {
    val jwtPrincipal = jwtVerifier.verify(session.token) ?: return null
    val member = memberCacheService.find(UUID.fromString(jwtPrincipal.getClaim("uuid").asString())) ?: return null

    return if (member.username == jwtPrincipal.getClaim("username").asString() &&
        member.roles.toTypedArray().contentEquals(jwtPrincipal.getClaim("roles").asArray(Role::class.java)) &&
        member.creationDateTime.toString() == jwtPrincipal.getClaim("creationDateTime").asString() &&
        member.roles.any { it == Role.ADMIN }
    ) session else null
}