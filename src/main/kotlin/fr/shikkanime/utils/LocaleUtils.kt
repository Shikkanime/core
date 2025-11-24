package fr.shikkanime.utils

import fr.shikkanime.entities.enums.CountryCode

object LocaleUtils {
    fun getConvertedLocales(locales: Collection<String?>) = locales.mapNotNull {
        when (it) {
            "日本語", "ja-jpn", "ja" -> "ja-JP"
            "English", "en-eng" -> "en-US"
            "Français", "Français (France)", "Français [CC]", "fr-fra" -> "fr-FR"
            else -> null
        }
    }.toSet()

    fun getAllowedLocales(countryCode: CountryCode, locales: Collection<String?>): Set<String> {
        val availableLocales = getConvertedLocales(locales)

        val matches = countryCode.allowedAudioLocales.intersect(availableLocales)
            .ifEmpty { countryCode.optionalAudioLocales.intersect(availableLocales) }

        return if (countryCode.locale in availableLocales) matches + countryCode.locale else matches
    }
}