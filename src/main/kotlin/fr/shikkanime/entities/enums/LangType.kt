package fr.shikkanime.entities.enums

enum class LangType {
    SUBTITLES,
    VOICE,
    ;

    companion object {
        fun fromAudioLocale(countryCode: CountryCode, audioLocale: String, ignoreCase: Boolean = true): LangType {
            return if (audioLocale.equals(countryCode.locale, ignoreCase)) VOICE else SUBTITLES
        }
    }
}