package fr.shikkanime.entities

data class Link(
    val href: String,
    val icon: String,
    val name: String,
    var active: Boolean = false,
) {
    companion object {
        fun list(): List<Link> {
            return listOf(
                Link("/admin/dashboard", "bi bi-speedometer2", "Dashboard"),
                Link("/admin/platforms", "bi bi-display", "Platforms"),
            )
        }
    }
}
