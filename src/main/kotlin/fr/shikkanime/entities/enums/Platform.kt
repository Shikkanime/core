package fr.shikkanime.entities.enums

enum class Platform(
    val platformName: String,
    var url: String,
    var image: String,
    var banner: String? = null,
) {
    ANIM("Animation Digital Network", "https://animationdigitalnetwork.fr/", "animation_digital_network.jpg", "animation_digital_network.png"),
    CRUN("Crunchyroll", "https://www.crunchyroll.com/", "crunchyroll.jpg", "crunchyroll.png"),
    DISN("Disney+", "https://www.disneyplus.com/", "disneyplus.jpg", "disneyplus.png"),
    NETF("Netflix", "https://www.netflix.com/", "netflix.jpg", "netflix.png"),
    PRIM("Prime Video", "https://www.primevideo.com/", "prime_video.jpg"),
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
