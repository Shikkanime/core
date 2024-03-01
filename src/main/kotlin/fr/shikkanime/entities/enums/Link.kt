package fr.shikkanime.entities.enums

import fr.shikkanime.utils.Constant

enum class Link(
    var href: String,
    val template: String,
    val icon: String,
    val label: String,
    val title: String = label,
    val footer: Boolean = false
) {
    // Admin
    DASHBOARD("/admin/dashboard", "/admin/dashboard.ftl", "bi bi-pc-display", "Dashboard"),
    PLATFORMS("/admin/platforms", "/admin/platforms/list.ftl", "bi bi-display", "Platforms"),
    ANIMES("/admin/animes", "/admin/animes/list.ftl", "bi bi-file-earmark-play", "Animes"),
    EPISODES("/admin/episodes", "/admin/episodes/list.ftl", "bi bi-collection-play", "Episodes"),
    CONFIG("/admin/config", "/admin/config/list.ftl", "bi bi-gear", "Configurations"),
    IMAGES("/admin/images", "/admin/images.ftl", "bi bi-images", "Images"),
    SIMULCASTS("/admin/simulcasts", "/admin/simulcasts.ftl", "bi bi-calendar-event", "Simulcasts"),

    // Site
    HOME("/", "/site/home.ftl", "", "Accueil", "${Constant.NAME} : Ne manquez plus jamais un épisode d'animé !"),
    CATALOG("/catalog/{currentSimulcast}", "/site/catalog.ftl", "", "Catalogue"),
    PRESENTATION("/presentation", "/site/presentation.ftl", "", "Présentation", footer = true),
    ;
}