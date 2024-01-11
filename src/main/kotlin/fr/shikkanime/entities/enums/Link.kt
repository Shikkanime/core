package fr.shikkanime.entities.enums

enum class Link(val href: String, val template: String, val icon: String, val label: String) {
    // Admin
    DASHBOARD("/admin/dashboard", "/admin/dashboard.ftl", "bi bi-pc-display", "Dashboard"),
    PLATFORMS("/admin/platforms", "/admin/platforms/list.ftl", "bi bi-display", "Platforms"),
    ANIMES("/admin/animes", "/admin/animes/list.ftl", "bi bi-file-earmark-play", "Animes"),
    EPISODES("/admin/episodes", "/admin/episodes/list.ftl", "bi bi-collection-play", "Episodes"),
    CONFIG("/admin/config", "/admin/config/list.ftl", "bi bi-gear", "Configurations"),

    // Site
    HOME("/", "/site/home.ftl", "", "Home"),
    CATALOG("/catalog", "/site/catalog.ftl", "", "Catalog"),
    ;
}