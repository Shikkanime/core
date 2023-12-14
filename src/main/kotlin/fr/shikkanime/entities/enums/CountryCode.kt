package fr.shikkanime.entities.enums


enum class CountryCode(val locale: String? = null, val voice: String? = null) {
    FR("fr-FR", "VF"),
    ;

    companion object {
        fun from(collection: Collection<String>): Set<CountryCode> {
            return collection.map { valueOf(it.uppercase()) }.toSet()
        }

        fun from(string: String): CountryCode {
            return valueOf(string.uppercase())
        }
    }
}