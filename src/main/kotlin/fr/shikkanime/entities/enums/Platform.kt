package fr.shikkanime.entities.enums

enum class Platform(
    val platformName: String,
    var url: String,
    var image: String
) {
    ANIM("Animation Digital Network", "https://animationdigitalnetwork.fr/", "animation_digital_network.jpg"),
    CRUN("Crunchyroll", "https://www.crunchyroll.com/", "crunchyroll.jpg"),
    DISN("Disney+", "https://www.disneyplus.com/", "disneyplus.jpg"),
    NETF("Netflix", "https://www.netflix.com/", "netflix.jpg"),
    ;

    companion object {
        fun findByName(name: String): Platform? {
            return entries.find { it.platformName == name }
        }
    }
}
