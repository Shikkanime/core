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

    /**
     * The locale spoken by the market's voice releases — the country's own
     * default locale (fr-FR for FR, de-DE for DE, …).
     */
    val voiceLocale: Locale
        get() = defaultLocale

    /**
     * Resolves which of [available] audio locales are visible for this market,
     * ported from the legacy `LocaleUtils.getAllowedLocales`:
     *
     * 1. the CJK subtitle locales ([SUBTITLE_LOCALES]) present in [available]
     *    are kept;
     * 2. when none is available, [FALLBACK_SUBTITLE_LOCALE] is kept instead;
     * 3. [voiceLocale] is always kept when available;
     * 4. every other locale is excluded.
     *
     * @param available The audio locales carried by one episode's releases.
     * @return The subset of [available] this market may display.
     */
    fun visibleAudioLocales(available: Set<Locale>): Set<Locale> {
        val subtitles = SUBTITLE_LOCALES.intersect(available).ifEmpty {
            setOfNotNull(FALLBACK_SUBTITLE_LOCALE.takeIf { it in available })
        }
        return subtitles + listOfNotNull(voiceLocale.takeIf { it in available })
    }

    companion object {
        /** Subtitle locales every market prefers, in priority order. */
        val SUBTITLE_LOCALES: Set<Locale> = setOf(Locale.JA_JP, Locale.ZH_CN, Locale.KO_KR)

        /** Subtitle locale used only when no [SUBTITLE_LOCALES] is available. */
        val FALLBACK_SUBTITLE_LOCALE: Locale = Locale.EN_US

        /**
         * Resolves a [Country] entry from its ISO 3166-1 alpha-2 code, ignoring case.
         *
         * @param code The country code to look up (e.g. "FR", "DE").
         * @return The matched [Country] instance, or `null` if unmapped.
         * @see [Country]
         */
        fun fromCode(code: String): Country? =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
