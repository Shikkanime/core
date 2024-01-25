package fr.shikkanime.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.dtos.TokenDto
import fr.shikkanime.entities.enums.Role
import fr.shikkanime.services.caches.MemberCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import java.util.*
import java.util.logging.Level

private val logger = LoggerFactory.getLogger("Security")

fun Application.configureSecurity() {
    val memberCacheService = Constant.injector.getInstance(MemberCacheService::class.java)

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
                return@validate validationSession(jwtVerifier, session, memberCacheService)
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

private fun validationSession(
    jwtVerifier: JWTVerifier,
    session: TokenDto,
    memberCacheService: MemberCacheService
): TokenDto? {
    try {
        val jwtPrincipal = jwtVerifier.verify(session.token)
        val uuid = UUID.fromString(jwtPrincipal.getClaim("uuid").asString())
        val username = jwtPrincipal.getClaim("username").asString()
        val creationDateTime = jwtPrincipal.getClaim("creationDateTime").asString()
        val role = Role.valueOf(jwtPrincipal.getClaim("role").asString())
        val member = memberCacheService.find(uuid) ?: return null

        if (member.username != username) {
            logger.log(Level.SEVERE, "Error while validating session: username mismatch")
            return null
        }

        if (member.role != role) {
            logger.log(Level.SEVERE, "Error while validating session: role mismatch")
            return null
        }

        if (member.creationDateTime.toString() != creationDateTime) {
            logger.log(Level.SEVERE, "Error while validating session: creationDateTime mismatch")
            return null
        }

        if (member.role != Role.ADMIN) {
            logger.log(Level.SEVERE, "Error while validating session: role is not admin")
            return null
        }

        return session
    } catch (e: Exception) {
        logger.log(Level.SEVERE, "Error while validating session", e)
        return null
    }
}
