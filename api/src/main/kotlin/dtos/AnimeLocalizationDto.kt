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
) {
    companion object {
        /**
         * Maps a stored localization to its API view.
         *
         * @param localization localized text read from the database.
         * @return the API view of [localization].
         * @see AnimeLocalization
         */
        fun toDto(localization: AnimeLocalization): AnimeLocalizationDto =
            AnimeLocalizationDto(
                locale = localization.locale,
                title = localization.title,
                description = localization.description
            )
    }
}
