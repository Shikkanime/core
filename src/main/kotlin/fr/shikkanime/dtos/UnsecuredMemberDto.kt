package fr.shikkanime.dtos

import fr.shikkanime.entities.enums.Role
import io.ktor.server.auth.*
import java.io.Serializable
import java.util.*

data class UnsecuredMemberDto(
    val uuid: UUID?,
    val creationDateTime: String,
    val username: String,
    val password: ByteArray,
    val role: Role,
) : Principal, Serializable
