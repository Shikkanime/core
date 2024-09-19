package fr.shikkanime.dtos.member

import io.ktor.server.auth.*

data class TokenDto(
    val token: String? = null,
) : Principal {
    companion object {
        val empty = TokenDto()
    }
}
