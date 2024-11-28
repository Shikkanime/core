package fr.shikkanime.entities.enums

enum class CountryCode(
    val locale: String,
    val timezone: String,
    val latitude: Double,
    val longitude: Double
) {
    FR("fr-FR", "Europe/Paris", 48.866667, 2.333333),
    ;

    companion object {
        fun from(collection: Collection<String>) = collection.map { valueOf(it.uppercase()) }

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