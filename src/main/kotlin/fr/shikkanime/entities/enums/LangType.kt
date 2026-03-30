package fr.shikkanime.entities.enums

enum class LangType {
    SUBTITLES,
    VOICE,
    ;

    companion object {
        fun valueOfNullable(string: String): LangType? = entries.find { it.name == string }

        fun fromAudioLocale(countryCode: CountryCode, audioLocale: String): LangType {
            return if (audioLocale.equals(countryCode.locale, true)) VOICE else SUBTITLES
        }
    }
}