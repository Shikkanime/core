package fr.shikkanime.models

enum class Locale(val code: String) {
    JA_JP("ja-JP"),
    EN_US("en-US"),
    FR_FR("fr-FR"),
    FR_BE("fr-BE"),
    FR_LU("fr-LU"),
    FR_CH("fr-CH"),
    CA_AD("ca-AD"),
    FR_MC("fr-MC"),
    DE_DE("de-DE"),
    PL_PL("pl-PL"),
    ZH_CN("zh-CN"),
    KO_KR("ko-KR")
    ;

    companion object {
        /**
         * Resolves a [Locale] entry from its BCP-47 language tag code, ignoring case.
         *
         * @param code The BCP-47 locale code to look up (e.g. "fr-FR", "ja-JP").
         * @return The matched [Locale] instance, or `null` if unmapped.
         */
        fun fromCode(code: String): Locale? =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
