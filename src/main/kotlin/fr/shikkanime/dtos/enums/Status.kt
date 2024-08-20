package fr.shikkanime.dtos.enums

enum class Status {
    VALID,
    INVALID,
    ;

    companion object {
        fun fromNullable(string: String?): Status? {
            return if (string == null) null else try {
                Status.valueOf(string.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}