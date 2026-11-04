package fr.shikkanime.admin.dtos.localization

import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.EpisodeLocalization
import fr.shikkanime.models.Locale
import kotlinx.serialization.Serializable

/**
 * Localized editorial text of an anime or an episode, as the administration reads it.
 */
@Serializable
data class AdminLocalizationDto(
    val locale: Locale,
    val title: String?,
    val description: String?
)

/**
 * Maps a stored anime localization to its administration view.
 *
 * @receiver stored localization.
 * @return the administration view of this localization.
 * @see [AdminLocalizationDto]
 */
fun AnimeLocalization.toAdminDto(): AdminLocalizationDto =
    AdminLocalizationDto(
        locale = locale,
        title = title,
        description = description
    )

/**
 * Maps a stored episode localization to its administration view.
 *
 * @receiver stored localization.
 * @return the administration view of this localization.
 * @see [AdminLocalizationDto]
 */
fun EpisodeLocalization.toAdminDto(): AdminLocalizationDto =
    AdminLocalizationDto(
        locale = locale,
        title = title,
        description = description
    )
