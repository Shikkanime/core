package fr.shikkanime.api.dtos

import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.Locale
import kotlinx.serialization.Serializable

/**
 * API view of a localized editorial text.
 */
@Serializable
data class AnimeLocalizationDto(
    val locale: Locale,
    val title: String,
    val description: String?
)

/**
 * Maps a stored localization to its API view.
 *
 * @receiver stored localization.
 * @return the API view of this localization.
 * @see [AnimeLocalizationDto]
 */
fun AnimeLocalization.toDto(): AnimeLocalizationDto =
    AnimeLocalizationDto(
        locale = locale,
        title = title,
        description = description
    )
