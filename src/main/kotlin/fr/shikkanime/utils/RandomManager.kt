package fr.shikkanime.utils

object RandomManager {
    fun generateRandomString(length: Int): String {
        val allowedChars =
            ('A'..'Z') + ('a'..'z') + ('0'..'9') + '_' + '-' + '.' + '!' + '~' + '*' + '\'' + '(' + ')' + ';' + ':' + '@' + '&' + '=' + '+' + '$' + ',' + '/' + '?' + '#' + '[' + ']' + '%'

        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}