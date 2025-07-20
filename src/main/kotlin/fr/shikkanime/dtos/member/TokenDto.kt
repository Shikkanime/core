package fr.shikkanime.dtos.member

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import fr.shikkanime.entities.Member
import fr.shikkanime.utils.Constant
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TokenDto(
    val token: String? = null,
) {
    companion object {
        val empty = TokenDto()

        fun build(member: Member) = TokenDto(
            JWT.create()
                .withAudience(Constant.jwtAudience)
                .withIssuer(Constant.jwtDomain)
                .withClaim("uuid", member.uuid.toString())
                .withClaim("isPrivate", member.isPrivate)
                .withClaim("username", member.username)
                .withClaim("creationDateTime", member.creationDateTime.toString())
                .withClaim("roles", member.roles.map { it.name })
                .withExpiresAt(Date(System.currentTimeMillis() + (1 * 60 * 60 * 1000)))
                .sign(Algorithm.HMAC256(Constant.jwtSecret))
        )
    }
}
