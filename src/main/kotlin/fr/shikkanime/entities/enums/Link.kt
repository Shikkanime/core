package fr.shikkanime.entities.enums

enum class Link(val href: String, val icon: String, val label: String) {
    DASHBOARD("/admin/dashboard", "bi bi-speedometer2", "Dashboard"),
    PLATFORMS("/admin/platforms", "bi bi-display", "Platforms"),
    ANIMES("/admin/animes", "bi bi-film", "Animes"),
    EPISODES("/admin/episodes", "bi bi-collection-play", "Episodes"),
    ;
}