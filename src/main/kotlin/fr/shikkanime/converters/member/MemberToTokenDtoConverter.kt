package fr.shikkanime.converters.member

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.TokenDto
import fr.shikkanime.entities.Member
import fr.shikkanime.utils.Constant
import java.util.*

class MemberToTokenDtoConverter : AbstractConverter<Member, TokenDto>() {
    override fun convert(from: Member): TokenDto {
        println(from)
        val token = JWT.create()
            .withAudience(Constant.jwtAudience)
            .withIssuer(Constant.jwtDomain)
            .withClaim("uuid", from.uuid.toString())
            .withClaim("username", from.username)
            .withClaim("creationDateTime", from.creationDateTime.toString())
            .withClaim("roles", from.roles.map { it.name })
            .withExpiresAt(Date(System.currentTimeMillis() + (1 * 60 * 60 * 1000)))
            .sign(Algorithm.HMAC256(Constant.jwtSecret))

        return TokenDto(token)
    }
}