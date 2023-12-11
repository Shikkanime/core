package fr.shikkanime.exceptions

data class AnimeNotSimulcastedException(override val message: String? = null) : AnimeException(message)
