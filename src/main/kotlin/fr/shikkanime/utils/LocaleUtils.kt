package fr.shikkanime.utils

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Locale

object LocaleUtils {
    private val LOCALE_MAP = mapOf(
        // Japanese
        "日本語" to Locale.JA_JP.code,
        "ja-jpn" to Locale.JA_JP.code,
        // English
        "English" to Locale.EN_US.code,
        "en-eng" to Locale.EN_US.code,
        // French
        "Français" to Locale.FR_FR.code,
        "Français (France)" to Locale.FR_FR.code,
        "Français [CC]" to Locale.FR_FR.code,
        "fr-fra" to Locale.FR_FR.code,
    )

    fun getConvertedLocales(locales: Collection<String?>): Set<String> {
        if (locales.isEmpty()) return emptySet()
        val result = mutableSetOf<String>()

        for (locale in locales) {
            if (locale.isNullOrBlank())
                continue

            result.add(LOCALE_MAP[locale] ?: locale)
        }

        return result
    }

    fun getAllowedLocales(countryCode: CountryCode, locales: Collection<String?>): Set<String> {
        val availableLocales = getConvertedLocales(locales)

        var matches = countryCode.allowedAudioLocales.filterTo(mutableSetOf()) {
            it in availableLocales
        }

        if (matches.isEmpty()) {
            matches = countryCode.optionalAudioLocales.filterTo(mutableSetOf()) {
                it in availableLocales
            }
        }

        if (countryCode.locale in availableLocales) {
            matches.add(countryCode.locale)
        }

        return matches
    }
}