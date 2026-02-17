package fr.shikkanime.entities.enums

enum class CountryCode(
    val allowedAudioLocales: Set<String>,
    val optionalAudioLocales: Set<String>,
    val locale: String,
    val timezone: String
) {
    FR(setOf("ja-JP", "zh-CN", "ko-KR"), setOf("en-US"), "fr-FR", "Europe/Paris"),
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