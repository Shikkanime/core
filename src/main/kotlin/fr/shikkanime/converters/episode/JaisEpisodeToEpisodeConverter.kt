package fr.shikkanime.converters.episode

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.jais.EpisodeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.EpisodeService
import java.time.ZonedDateTime

class JaisEpisodeToEpisodeConverter : AbstractConverter<EpisodeDto, Episode>() {
    @Inject
    private lateinit var episodeService: EpisodeService

    override fun convert(from: EpisodeDto): Episode {
        val anime = convert(from.anime, Anime::class.java)
        val platform = Platform.findByName(from.platform.name)!!
        val split = from.hash.split('-')
        val id = (1..(split.size - 2)).joinToString("-") { split[it] }
        val hash = "${anime.countryCode}-$platform-$id"
        val foundEpisode = episodeService.findByHash(hash)

        if (foundEpisode != null) {
            return foundEpisode
        }

        val langType = LangType.valueOf(from.langType.name)

        return Episode(
            platform = platform,
            anime = anime,
            episodeType = EpisodeType.valueOf(from.episodeType.name),
            langType = langType,
            hash = "$hash-${langType.name}",
            releaseDateTime = ZonedDateTime.parse(from.releaseDate),
            season = from.season,
            number = from.number,
            title = from.title,
            url = from.url,
            image = from.image,
            duration = from.duration,
        )
    }
}