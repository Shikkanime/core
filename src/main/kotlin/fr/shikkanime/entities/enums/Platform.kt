package fr.shikkanime.entities.enums

enum class Platform(
    val platformName: String,
    var url: String,
    var image: String
) {
    ANIM("Animation Digital Network", "https://animationdigitalnetwork.fr/", "animation_digital_network.png"),
    CRUN("Crunchyroll", "https://www.crunchyroll.com/", "crunchyroll.png"),
    DISN("Disney+", "https://www.disneyplus.com/", "disneyplus.png"),
    NETF("Netflix", "https://www.netflix.com/", "netflix.png"),
    ;

    companion object {
        fun findByName(name: String): Platform? {
            return entries.find { it.platformName == name }
        }
    }
}
