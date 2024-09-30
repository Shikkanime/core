package fr.shikkanime.entities.enums

enum class Platform(
    val platformName: String,
    var url: String,
    var image: String,
    val sortIndex: Short,
) {
    ANIM("Animation Digital Network", "https://animationdigitalnetwork.fr/", "animation_digital_network.jpg", 1),
    CRUN("Crunchyroll", "https://www.crunchyroll.com/", "crunchyroll.jpg", 0),
    DISN("Disney+", "https://www.disneyplus.com/", "disneyplus.jpg", 2),
    NETF("Netflix", "https://www.netflix.com/", "netflix.jpg", 3),
    PRIM("Prime Video", "https://www.primevideo.com/", "prime_video.jpg", 4),
    ;

    companion object {
        fun findByName(name: String): Platform? {
            return entries.find { it.platformName == name }
        }

        fun fromNullable(string: String?): Platform? {
            return if (string == null) null else try {
                Platform.valueOf(string.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
