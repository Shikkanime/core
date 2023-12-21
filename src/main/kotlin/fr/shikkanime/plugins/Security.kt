package fr.shikkanime.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import fr.shikkanime.dtos.MemberDto
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.entities.enums.Role
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*

fun Application.configureSecurity() {
    // Please read the jwt property from the config file if you are using EngineMain
    val jwtAudience = "jwt-audience"
    val jwtDomain = "https://jwt-provider-domain/"
    val jwtRealm = "ktor sample app"
    val jwtSecret = "secret"

    val memberService = Constant.injector.getInstance(MemberService::class.java)

    authentication {
        jwt {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }

        session<MemberDto>("auth-admin-session") {
            validate { session ->
                if (session.uuid == null) return@validate null
                val user = memberService.find(session.uuid) ?: return@validate null
                if (user.role != Role.ADMIN) return@validate null
                return@validate session
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized, MessageDto(MessageDto.Type.ERROR, "You are not authorized to access this page"))
            }
        }
    }

    install(Sessions) {
        cookie<MemberDto>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 3600
        }
    }
}
