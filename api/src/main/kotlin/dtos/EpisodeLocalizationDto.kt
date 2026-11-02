package fr.shikkanime.api.dtos

import fr.shikkanime.models.EpisodeLocalization
import fr.shikkanime.models.Locale
import kotlinx.serialization.Serializable

/**
 * API view of a localized episode text.
 */
@Serializable
data class EpisodeLocalizationDto(
    val locale: Locale,
    val title: String?,
    val description: String?
)

/**
 * Maps a stored episode localization to its API view.
 *
 * @receiver stored localization.
 * @return the API view of this localization.
 * @see [EpisodeLocalizationDto]
 */
fun EpisodeLocalization.toDto(): EpisodeLocalizationDto =
    EpisodeLocalizationDto(
        locale = locale,
        title = title,
        description = description
    )
