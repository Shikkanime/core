package fr.shikkanime.repositories

import fr.shikkanime.entities.Episode

@Deprecated("Use EpisodeMappingRepository instead")
class EpisodeRepository : AbstractRepository<Episode>() {
    override fun getEntityClass() = Episode::class.java
}