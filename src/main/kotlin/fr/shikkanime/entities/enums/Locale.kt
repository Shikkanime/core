package fr.shikkanime.entities.enums

enum class Locale {
    JA_JP,
    EN_US,
    FR_FR,
    ZH_CH,
    KO_KR
    ;

    val code: String
        get() {
            val split = name.split("_")
            return "${split[0].lowercase()}-${split[1]}"
        }
}