package fr.shikkanime.models

/**
 * Broad language role of an episode release for a given market.
 *
 * Ported from the legacy `LangType`: every release is either a subtitle track
 * (CJK locales, or English as fallback) or the country's own voice locale.
 * The classification is mechanical — a locale that is not the country's voice
 * is [SUBTITLES] even when it is not visible for that market; visibility is
 * resolved upstream by [Country.visibleAudioLocales].
 *
 * @see [Country.visibleAudioLocales]
 * @see [EpisodeGrouper]
 */
enum class LangType {
    SUBTITLES,
    VOICE;

    companion object {
        /**
         * Classifies [audioLocale] for [country]: [VOICE] when it is the
         * country's own voice locale, [SUBTITLES] otherwise.
         */
        fun fromAudioLocale(country: Country, audioLocale: Locale): LangType =
            if (audioLocale == country.voiceLocale) VOICE else SUBTITLES
    }
}
