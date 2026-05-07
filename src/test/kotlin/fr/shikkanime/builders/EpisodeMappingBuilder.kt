package fr.shikkanime.builders

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.services.EpisodeMappingService
import java.util.UUID

class EpisodeMappingBuilder : AbstractEntityBuilder<EpisodeMapping, EpisodeMappingService>() {
    var uuid: UUID? = null
    var anime: Anime? = null
    var season: Int? = null
    var episodeType: EpisodeType? = null
    var number: Int? = null

    override fun buildEntity() = EpisodeMapping(
        uuid  = uuid,
        anime = anime,
        season = season,
        episodeType = episodeType,
        number = number
    )
}