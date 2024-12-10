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
    TRACE_ACTIONS("/admin/trace-actions", "/admin/trace-actions.ftl", "bi bi-database-exclamation", "Trace actions"),
    MEMBERS("/admin/members", "/admin/members/list.ftl", "bi bi-people", "Members"),
    RULES("/admin/rules", "/admin/rules.ftl", "bi bi-rulers", "Rules"),
    THREADS("/admin/threads", "/admin/threads.ftl", "bi bi-threads", "Threads"),
    CONFIG("/admin/config", "/admin/configs.ftl", "bi bi-gear", "Configurations"),

    // Site
    HOME("/", "/site/home.ftl", "", "Accueil", "${Constant.NAME} : Ne manquez plus jamais un épisode d'animé !"),
    CATALOG("/catalog/{currentSimulcast}", "/site/catalog.ftl", "", "Catalogue"),
    CALENDAR("/calendar", "/site/calendar.ftl", "", "Calendrier"),
    SEARCH("/search", "/site/search.ftl", "", "Recherche"),
    PRESENTATION("/presentation", "/site/presentation.ftl", "", "Présentation", footer = true),
    PRIVACY("/privacy", "/site/privacy.ftl", "", "Politique de confidentialité", footer = true),
    ;
}