package fr.shikkanime.exceptions

data class EpisodeNotAvailableInCountryException(override val message: String? = null) : EpisodeException(message)
