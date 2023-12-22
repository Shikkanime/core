package fr.shikkanime.converters.episode

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.services.EpisodeService
import java.time.ZonedDateTime

class EpisodeDtoToEpisodeConverter : AbstractConverter<EpisodeDto, Episode>() {
    @Inject
    private lateinit var episodeService: EpisodeService

    override fun convert(from: EpisodeDto): Episode {
        val foundEpisode = episodeService.findByHash(from.hash)

        if (foundEpisode != null) {
            return foundEpisode
        }

        return Episode(
            platform = from.platform,
            anime = convert(from.anime, Anime::class.java),
            episodeType = from.episodeType,
            langType = from.langType,
            hash = from.hash,
            releaseDateTime = ZonedDateTime.parse(from.releaseDateTime),
            season = from.season,
            number = from.number,
            title = from.title,
            url = from.url,
            image = from.image,
            duration = from.duration,
        )
    }
}