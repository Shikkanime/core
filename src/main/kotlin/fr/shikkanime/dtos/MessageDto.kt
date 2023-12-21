package fr.shikkanime.dtos

import java.io.Serializable

data class MessageDto(
    val type: Type,
    val message: String,
    val data: Any? = null,
) : Serializable {
    enum class Type {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }
}
