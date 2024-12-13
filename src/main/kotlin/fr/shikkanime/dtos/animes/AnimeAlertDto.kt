package fr.shikkanime.dtos.animes


data class AnimeAlertDto(
    val anime: AnimeDto,
    val zonedDateTime: String,
    val errors: Set<AnimeError>,
)

enum class ErrorType {
    INVALID_CHAIN_SEASON,
    INVALID_RELEASE_DATE,
    INVALID_EPISODE_NUMBER,
    INVALID_CHAIN_EPISODE_NUMBER,
}

data class AnimeError(
    val type: ErrorType,
    val reason: String,
)