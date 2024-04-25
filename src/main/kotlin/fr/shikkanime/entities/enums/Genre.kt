package fr.shikkanime.entities.enums

enum class Genre(vararg aliases: String) {
    ACTION("action"),
    ADVENTURE("aventure", "adventure"),
    BIOGRAPHICAL("biographique", "biographical"),
    COMEDY("comédie", "comedie", "comedy"),
    DELINQUENT("delinquant", "delinquent", "delinquents"),
    DETECTIVE("détective", "detective"),
    DRAMA("drame", "drama"),
    ECCHI("ecchi"),
    FANTASY("fantasy", "fantaisie", "fantastique"),
    GORE("gore"),
    GOURMET("gourmet"),
    HAREM("harem"),
    HISTORY("historique", "history", "historical"),
    HORROR("horreur", "horror", "horreur / épouvante"),
    HUMOR("humour", "humor"),
    IDOL("idols", "idols (male)", "idols (female)"),
    ISEKAI("isekai"),
    MAGICAL_GIRL("magical girl"),
    MARTIAL_ARTS("arts martiaux", "martial arts"),
    MATURE("mature"),
    MECHA("mecha"),
    MEDICAL("médical", "medical"),
    MILITARY("militaire", "military"),
    MOE("moe"),
    MUSIC("musique", "music"),
    MYTHOLOGY("mythologie", "mythology"),
    MYSTERY("mystère", "mystery"),
    NEKKETSU("nekketsu"),
    PARODY("parodie", "parody"),
    PET("pet", "pets"),
    PSYCHOLOGICAL("psychologique", "psychological"),
    RACING("racing"),
    REINCARNATION("reincarnation"),
    ROMANCE("romance"),
    SAMURAI("samouraï", "samurai"),
    SCHOOL_LIFE("school life", "school"),
    SCIENCE_FANTASY("science-fantasy"),
    SCIENCE_FICTION("science-fiction", "science fiction", "sci-fi"),
    SHOJO("shojo", "shôjo"),
    SHOJO_AI("shojo ai", "shôjo-ai"),
    SHONEN("shonen", "shônen"),
    SHONEN_AI("shonen ai", "shônen-ai"),
    SEINEN("seinen"),
    SLICE_OF_LIFE("slice of life", "tranche de vie"),
    SPACE("space"),
    SPORT("sport", "sports", "team sports"),
    STRATEGY_GAME("strategy game", "jeu de stratégie"),
    SUPERNATURAL("surnaturel", "supernatural"),
    SUPER_POWER("super power", "super-pouvoir"),
    SURVIVAL("survival"),
    SUSPENSE("suspense"),
    TIME_TRAVEL("time travel", "voyage dans le temps"),
    THRILLER("thriller"),
    VIDEO_GAME("video game", "jeu vidéo"),
    WORKPLACE("workplace"),
    YAOI("yaoi"),
    ;

    private val aliases = aliases.toSet()

    companion object {
        fun from(string: String): Genre? {
            return entries.find { it.aliases.map { alias -> alias.lowercase() }.contains(string.lowercase()) }
        }
    }
}
