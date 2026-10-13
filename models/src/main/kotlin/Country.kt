package fr.shikkanime.models

enum class Country(
    val code: String,
    val defaultLocale: Locale
) {
    FR("FR", Locale.FR_FR),
    BE("BE", Locale.FR_BE),
    LU("LU", Locale.FR_LU),
    CH("CH", Locale.FR_CH),
    AD("AD", Locale.CA_AD),
    MC("MC", Locale.FR_MC),
    DE("DE", Locale.DE_DE),
    PL("PL", Locale.PL_PL);

    companion object {
        /**
         * Resolves a [Country] entry from its ISO 3166-1 alpha-2 code, ignoring case.
         *
         * @param code The country code to look up (e.g. "FR", "DE").
         * @return The matched [Country] instance, or `null` if unmapped.
         */
        fun fromCode(code: String): Country? =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
