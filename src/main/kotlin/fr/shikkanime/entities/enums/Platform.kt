package fr.shikkanime.entities.enums

enum class Platform(
    val platformName: String,
    var url: String,
    var image: String,
    val sortIndex: Short,
    val isStreamingPlatform: Boolean,
) {
    ANIM("Animation Digital Network", "https://animationdigitalnetwork.fr/", "animation_digital_network.jpg", 0, true),
    CRUN("Crunchyroll", "https://www.crunchyroll.com/", "crunchyroll.jpg", 1, true),
    DISN("Disney+", "https://www.disneyplus.com/", "disneyplus.jpg", 2, true),
    NETF("Netflix", "https://www.netflix.com/", "netflix.jpg", 3, true),
    PRIM("Prime Video", "https://www.primevideo.com/", "prime_video.jpg", 4, true),
    // Non-streaming platforms
    ANIL("AniList", "https://anilist.co/", "anilist.jpg", -1, false),
    ;

    companion object {
        fun findByName(name: String?): Platform? {
            return entries.find { it.platformName == name }
        }

        fun fromNullable(string: String?): Platform? {
            return if (string == null) null else try {
                Platform.valueOf(string.uppercase())
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}
