package fr.shikkanime.entities.enums

enum class CountryCode(val locale: String, val timezone: String) {
    FR("fr-FR", "Europe/Paris"),
    ;

    companion object {
        fun from(collection: Collection<String>): Set<CountryCode> {
            return collection.map { valueOf(it.uppercase()) }.toSet()
        }

        fun from(string: String): CountryCode {
            return valueOf(string.uppercase())
        }

        fun fromNullable(string: String?): CountryCode? {
            return if (string == null) null else try {
                valueOf(string.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}