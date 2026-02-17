package fr.shikkanime.dtos

data class MessageDto(
    val type: Type,
    val message: String,
    val data: Any? = null,
) {
    enum class Type {
        ERROR
    }

    companion object {
        fun error(message: String, data: Any? = null) = MessageDto(Type.ERROR, message, data)
    }
}
