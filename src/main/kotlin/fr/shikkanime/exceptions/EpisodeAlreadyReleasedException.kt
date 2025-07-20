package fr.shikkanime.exceptions

data class EpisodeAlreadyReleasedException(override val message: String? = null) : EpisodeException(message)
