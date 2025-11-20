package fr.shikkanime.entities.enums

import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.StringUtils

enum class Link(
    var href: String,
    val template: String,
    val icon: String,
    val label: String,
    val title: String = label,
    val footer: Boolean = false
) {
    // Admin
    DASHBOARD("$ADMIN/dashboard", "/admin/dashboard.ftl", "bi bi-pc-display", "Dashboard"),
    PLATFORMS("$ADMIN/platforms", "/admin/platforms/list.ftl", "bi bi-display", "Platforms"),
    ANIMES("$ADMIN/animes", "/admin/animes/list.ftl", "bi bi-file-earmark-play", "Animes"),
    EPISODES("$ADMIN/episodes", "/admin/episodes/list.ftl", "bi bi-collection-play", "Episodes"),
    EPISODE_MANAGER("$ADMIN/episode-manager", "/admin/episode-manager.ftl", "bi bi-kanban", "Episode manager"),
    TRACE_ACTIONS("$ADMIN/trace-actions", "/admin/trace-actions.ftl", "bi bi-database-exclamation", "Trace actions"),
    MEMBERS("$ADMIN/members", "/admin/members/list.ftl", "bi bi-people", "Members"),
    RULES("$ADMIN/rules", "/admin/rules.ftl", "bi bi-rulers", "Rules"),
    JOBS("$ADMIN/jobs", "/admin/jobs.ftl", "bi bi-gear-wide-connected", "Jobs"),
    EMAILS("$ADMIN/emails", "/admin/emails.ftl", "bi bi-envelope", "Emails"),
    THREADS("$ADMIN/threads", "/admin/threads.ftl", "bi bi-threads", "Threads"),
    CONFIG("$ADMIN/config", "/admin/configs.ftl", "bi bi-gear", "Configurations"),

    // Site
    HOME("/", "/site/home.ftl", StringUtils.EMPTY_STRING, "Accueil", "${Constant.NAME} : Ne manquez plus jamais un épisode d'animé !"),
    CATALOG("/catalog/{currentSimulcast}", "/site/catalog.ftl", StringUtils.EMPTY_STRING, "Catalogue"),
    CALENDAR("/calendar", "/site/calendar.ftl", StringUtils.EMPTY_STRING, "Calendrier"),
    SEARCH("/search", "/site/search.ftl", StringUtils.EMPTY_STRING, "Recherche"),
    PRESENTATION("/presentation", "/site/presentation.ftl", StringUtils.EMPTY_STRING, "Présentation", footer = true),
    PRIVACY("/privacy", "/site/privacy.ftl", StringUtils.EMPTY_STRING, "Politique de confidentialité", footer = true),
    ;
}