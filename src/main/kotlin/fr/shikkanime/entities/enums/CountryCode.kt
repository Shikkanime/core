package fr.shikkanime.entities.enums

enum class CountryCode(
    val allowedAudioLocales: Set<String>,
    val optionalAudioLocales: Set<String>,
    val locale: String,
    val timezone: String
) {
    FR(
        setOf(Locale.JA_JP.code, Locale.ZH_CH.code, Locale.KO_KR.code),
        setOf(Locale.EN_US.code),
        Locale.FR_FR.code,
        "Europe/Paris"
    ),
    ;

    companion object {
        fun from(collection: Collection<String>) = collection.map { valueOf(it.uppercase()) }

        fun from(string: String): CountryCode {
            return valueOf(string.uppercase())
        }

        fun fromNullable(string: String?): CountryCode? {
            return if (string == null) null else try {
                valueOf(string.uppercase())
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}