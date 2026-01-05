package fr.shikkanime.services.seo

import com.google.gson.annotations.SerializedName
import fr.shikkanime.dtos.AnimePlatformDto
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.SeasonDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.ObjectParser

class JsonLdBuilder {
    private data class TvSeriesJsonLd(
        @SerializedName("@context") val context: String = "https://schema.org",
        @SerializedName("@type") val type: String = "TVSeries",
        val name: String,
        val alternateName: String,
        val url: String,
        val thumbnailUrl: String,
        val image: String,
        val description: String?,
        val startDate: String,
        val dateModified: String,
        val inLanguage: Collection<String>?,
        val provider: Collection<ProviderJsonLd>?,
        val numberOfEpisodes: Long,
        val numberOfSeasons: Int,
        val containsSeason: Collection<SeasonJsonLd>?,
        val keywords: String?,
    )

    private data class ProviderJsonLd(
        @SerializedName("@type") val type: String = "Organization",
        val name: String,
        val url: String,
        val logo: String,
    )

    private data class SeasonJsonLd(
        @SerializedName("@type") val type: String = "TVSeason",
        val name: String,
        val seasonNumber: Int,
        val startDate: String,
        val dateModified: String,
        val numberOfEpisodes: Long,
    )

    fun build(anime: AnimeDto, seasons: Set<SeasonDto>, platforms: Set<AnimePlatformDto>): String {
        val sortedSeasons = seasons.sortedBy { it.number }
        val containsSeason = sortedSeasons.map { season ->
            SeasonJsonLd(
                name = "Saison ${season.number}",
                seasonNumber = season.number,
                startDate = season.releaseDateTime,
                dateModified = season.lastReleaseDateTime,
                numberOfEpisodes = season.episodes
            )
        }

        val totalEpisodes = sortedSeasons.sumOf { it.episodes }

        val uniqueProviders = platforms
            .map(AnimePlatformDto::platform)
            .distinctBy(PlatformDto::id)
            .map { platform ->
                ProviderJsonLd(
                    name = platform.name,
                    url = platform.url,
                    logo = "${Constant.baseUrl}/assets/img/platforms/${platform.image}",
                )
            }

        val jsonLd = TvSeriesJsonLd(
            name = anime.name,
            alternateName = anime.shortName,
            url = "${Constant.baseUrl}/animes/${anime.slug}",
            thumbnailUrl = "${Constant.apiUrl}/v1/attachments?uuid=${anime.uuid}&type=THUMBNAIL",
            image = "${Constant.apiUrl}/v1/attachments?uuid=${anime.uuid}&type=BANNER",
            description = anime.description,
            startDate = anime.releaseDateTime,
            dateModified = anime.lastUpdateDateTime,
            inLanguage = anime.audioLocales,
            provider = uniqueProviders.takeIf { it.isNotEmpty() },
            numberOfEpisodes = totalEpisodes,
            numberOfSeasons = sortedSeasons.size,
            containsSeason = containsSeason.takeIf { it.isNotEmpty() },
            keywords = anime.simulcasts?.joinToString(", ") { it.label }
        )

        return ObjectParser.toJson(jsonLd)
    }
}
