package fr.shikkanime.entities.miscellaneous

import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.entities.enums.Link

data class LinkObject(
    var href: String,
    val icon: String,
    val name: String,
    var active: Boolean = false,
    val footer: Boolean = false
) {
    companion object {
        val adminList = Link.entries.filter { it.href.startsWith(ADMIN) && !it.footer }.map { LinkObject(it.href, it.icon, it.label, it.footer) }
        val adminFooterList = Link.entries.filter { it.href.startsWith(ADMIN) && it.footer }.map { LinkObject(it.href, it.icon, it.label, it.footer) }

        val siteList = Link.entries.filter { !it.href.startsWith(ADMIN) && !it.footer }.map { LinkObject(it.href, it.icon, it.label, it.footer) }
        val siteFooterList = Link.entries.filter { !it.href.startsWith(ADMIN) && it.footer }.map { LinkObject(it.href, it.icon, it.label, it.footer) }
    }
}