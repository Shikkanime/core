package fr.shikkanime.dtos

import fr.shikkanime.entities.enums.Role
import io.ktor.server.auth.*
import java.io.Serializable
import java.util.*

data class MemberDto(
    val uuid: UUID?,
    val creationDateTime: String,
    val username: String,
    val role: Role,
) : Principal, Serializable {
    companion object {
        val empty = MemberDto(null, "", "", Role.GUEST)
    }
}
