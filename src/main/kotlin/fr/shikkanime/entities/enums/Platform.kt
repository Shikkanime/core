package fr.shikkanime.entities.enums

enum class Platform(
    val platformName: String,
    var url: String,
    var image: String,
    val sortIndex: Short,
    val isStreamingPlatform: Boolean,
    val seriesUrl: String
) {
    ANIM("Animation Digital Network", "https://animationdigitalnetwork.fr/", "animation_digital_network.jpeg", 0, true, "https://animationdigitalnetwork.com/video/{ID}"),
    CRUN("Crunchyroll", "https://www.crunchyroll.com/", "crunchyroll.jpg", 1, true, "https://www.crunchyroll.com/series/{ID}"),
    DISN("Disney+", "https://www.disneyplus.com/", "disneyplus.jpg", 2, true, "https://www.disneyplus.com/browse/entity-{ID}"),
    NETF("Netflix", "https://www.netflix.com/", "netflix.jpg", 3, true, "https://www.netflix.com/title/{ID}"),
    PRIM("Prime Video", "https://www.primevideo.com/", "prime_video.jpg", 4, true, "https://www.primevideo.com/detail/{ID}"),
    // Non-streaming platforms
    ANIL("AniList", "https://anilist.co/", "anilist.jpg", -1, false, "https://anilist.co/anime/{ID}"),
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
