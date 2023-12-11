package fr.shikkanime.entities.enums

enum class Platform(
    val platformName: String,
    var url: String,
    var image: String
) {
    ANIM("Animation Digital Network", "https://animationdigitalnetwork.fr/", "animation_digital_network.png"),
    CRUN("Crunchyroll", "https://www.crunchyroll.com/", "crunchyroll.png"),
    DISN("Disney+", "https://www.disneyplus.com/", "disneyplus.png"),
    ;
}
