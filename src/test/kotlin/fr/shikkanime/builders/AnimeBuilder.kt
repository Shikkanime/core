package fr.shikkanime.builders

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.AnimeService
import java.util.UUID

class AnimeBuilder : AbstractEntityBuilder<Anime, AnimeService>() {
    var uuid: UUID? = null
    var countryCode: CountryCode? = null
    var name: String? = null
    var slug: String? = null
    var episodes: List<EpisodeMappingBuilder>? = null

    override fun buildEntity() = Anime(
        uuid = uuid,
        countryCode = countryCode,
        name = name,
        slug = slug ?: name
    )

    override suspend fun build(): Anime {
        val anime = super.build()
        episodes?.forEach { episode ->
            episode.anime = anime
            episode.build()
        }
        return anime
    }
}