package fr.shikkanime.dtos

import io.ktor.server.auth.*
import java.io.Serializable

data class TokenDto(
    val token: String? = null,
) : Principal, Serializable {
    companion object {
        val empty = TokenDto()
    }
}
