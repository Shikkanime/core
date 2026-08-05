package fr.shikkanime.utils

object RandomManager {
    private const val RANDOM_STRING_CHARACTERS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-.'!~*'();:@&=+$,/?#[]%"

    fun generateRandomString(length: Int): String {
        require(length >= 0) { "Length must not be negative" }

        return buildString(length) {
            repeat(length) {
                append(RANDOM_STRING_CHARACTERS.random())
            }
        }
    }
}