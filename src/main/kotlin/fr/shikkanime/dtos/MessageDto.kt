package fr.shikkanime.dtos

data class MessageDto(
    val type: Type,
    val message: String,
    val data: Any? = null,
) {
    enum class Type {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }
}
