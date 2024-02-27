package fr.shikkanime.entities

import fr.shikkanime.entities.enums.Link

data class LinkObject(
    var href: String,
    val icon: String,
    val name: String,
    var active: Boolean = false,
) {
    companion object {
        fun list(): List<LinkObject> {
            return Link.entries.map { LinkObject(it.href, it.icon, it.label) }
        }
    }
}
