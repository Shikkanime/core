package fr.shikkanime.exceptions

data class EpisodeNoSubtitlesOrVoiceException(override val message: String? = null) : EpisodeException(message)
