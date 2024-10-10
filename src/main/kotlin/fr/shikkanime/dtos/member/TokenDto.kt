package fr.shikkanime.dtos.member

import kotlinx.serialization.Serializable

@Serializable
data class TokenDto(
    val token: String? = null,
) {
    companion object {
        val empty = TokenDto()
    }
}
