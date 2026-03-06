package fr.shikkanime.services.seo

import com.google.gson.annotations.SerializedName
import fr.shikkanime.dtos.AnimePlatformDto
import fr.shikkanime.dtos.GenreDto
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.dtos.mappings.GroupedEpisodeDto
import fr.shikkanime.dtos.weekly.WeeklyAnimeDto
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.takeIfNotEmpty

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
        val genre: List<String>?,
        val keywords: List<String>?,
    )

    private data class TvEpisodeJsonLd(
        @SerializedName("@context") val context: String = "https://schema.org",
        @SerializedName("@type") val type: String = "TVEpisode",
        val name: String,
        val episodeNumber: String,
        val description: String?,
        val image: String?,
        val datePublished: String,
        val dateModified: String,
        val partOfSeason: SeasonJsonLd?,
        val partOfSeries: SeriesPartJsonLd?,
        val duration: String?,
        val potentialAction: List<PotentialActionJsonLd>?,
    )

    private data class SeriesPartJsonLd(
        @SerializedName("@type") val type: String = "TVSeries",
        val name: String,
        val url: String,
    )

    private data class PotentialActionJsonLd(
        @SerializedName("@type") val type: String = "WatchAction",
        val target: String,
        val actionAccessibilityRequirement: ActionAccessibilityRequirementJsonLd?,
    )

    private data class ActionAccessibilityRequirementJsonLd(
        @SerializedName("@type") val type: String = "ActionAccessSpecification",
        val category: String,
        val availabilityStarts: String,
        val availabilityEnds: String?,
        val requiresSubscription: Boolean,
        val eligibleRegion: List<CountryJsonLd>?,
    )

    private data class CountryJsonLd(
        @SerializedName("@type") val type: String = "Country",
        val name: String,
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
        val numberOfEpisodes: Long? = null,
    )

    fun build(anime: AnimeDto): String {
        val sortedSeasons = anime.seasons!!.sortedBy { it.number }
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

        val uniqueProviders = anime.platformIds!!
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
            provider = uniqueProviders.takeIfNotEmpty(),
            numberOfEpisodes = totalEpisodes,
            numberOfSeasons = sortedSeasons.size,
            containsSeason = containsSeason.takeIfNotEmpty(),
            genre = anime.genres?.map(GenreDto::name)?.takeIfNotEmpty(),
            keywords = buildList {
                anime.simulcasts?.map(SimulcastDto::label)?.let(::addAll)
                anime.tags?.map { it.tag.name }?.let(::addAll)
            }.takeIfNotEmpty()
        )

        return ObjectParser.toJson(jsonLd)
    }

    private fun formatDuration(duration: Long?): String? {
        if (duration == null || duration <= 0) return null
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60
        return buildString {
            append("PT")
            if (hours > 0) append("${hours}H")
            if (minutes > 0) append("${minutes}M")
            if (seconds > 0) append("${seconds}S")
        }
    }

    private fun buildPotentialAction(url: String, platform: PlatformDto, releaseDateTime: String, countryName: String): PotentialActionJsonLd {
        return PotentialActionJsonLd(
            target = url,
            actionAccessibilityRequirement = ActionAccessibilityRequirementJsonLd(
                category = if (platform.requiresSubscription) "subscription" else "free",
                availabilityStarts = releaseDateTime,
                availabilityEnds = null,
                requiresSubscription = platform.requiresSubscription,
                eligibleRegion = listOf(CountryJsonLd(name = countryName))
            )
        )
    }

    private fun buildTvEpisodeJsonLd(
        name: String,
        episodeNumber: String,
        seasonNumber: Int?,
        description: String?,
        image: String?,
        datePublished: String,
        dateModified: String,
        anime: AnimeDto,
        duration: Long?,
        potentialActions: List<PotentialActionJsonLd>?
    ): String {
        val jsonLd = TvEpisodeJsonLd(
            name = name,
            episodeNumber = episodeNumber,
            description = description,
            image = image,
            datePublished = datePublished,
            dateModified = dateModified,
            partOfSeason = seasonNumber?.let {
                SeasonJsonLd(
                    name = "Saison $it",
                    seasonNumber = it,
                    startDate = datePublished,
                    dateModified = dateModified
                )
            },
            partOfSeries = SeriesPartJsonLd(
                name = anime.name,
                url = "${Constant.baseUrl}/animes/${anime.slug}"
            ),
            duration = formatDuration(duration),
            potentialAction = potentialActions
        )
        return ObjectParser.toJson(jsonLd)
    }

    fun build(episode: EpisodeMappingDto): String {
        val anime = episode.anime ?: return ""
        return buildTvEpisodeJsonLd(
            name = episode.title ?: "${anime.name} - ${StringUtils.toEpisodeMappingString(episode, showSeason = false, separator = false)}",
            episodeNumber = episode.number.toString(),
            seasonNumber = episode.season,
            description = episode.description,
            image = episode.image ?: episode.uuid?.let { "${Constant.apiUrl}/v1/attachments?uuid=$it&type=BANNER" },
            datePublished = episode.releaseDateTime,
            dateModified = episode.lastUpdateDateTime,
            anime = anime,
            duration = episode.duration,
            potentialActions = episode.sources.map { buildPotentialAction(it.url, it.platform, episode.releaseDateTime, anime.countryCode?.name ?: "FR") }
        )
    }

    fun build(episode: GroupedEpisodeDto): String {
        val anime = episode.anime
        val firstMappingUuid = episode.mappings.firstOrNull()
        return buildTvEpisodeJsonLd(
            name = episode.title ?: "${anime.name} - ${StringUtils.toEpisodeGroupedString(episode, showSeason = false, separator = false)}",
            episodeNumber = episode.number,
            seasonNumber = episode.season.toIntOrNull(),
            description = episode.description,
            image = firstMappingUuid?.let { "${Constant.apiUrl}/v1/attachments?uuid=$it&type=BANNER" },
            datePublished = episode.releaseDateTime,
            dateModified = episode.lastUpdateDateTime,
            anime = anime,
            duration = episode.duration,
            potentialActions = episode.sources.map { buildPotentialAction(it.url, it.platform, episode.releaseDateTime, anime.countryCode?.name ?: "FR") }
        )
    }

    fun build(episode: WeeklyAnimeDto): String {
        val anime = episode.anime
        val mappings = episode.mappings?.takeIfNotEmpty() ?: return ""
        val firstMapping = mappings.first()

        val numberStr = when {
            episode.number != null -> episode.number.toString()
            episode.minNumber != null && episode.maxNumber != null -> if (episode.minNumber == episode.maxNumber) episode.minNumber.toString() else "${episode.minNumber} - ${episode.maxNumber}"
            else -> ""
        }

        return buildTvEpisodeJsonLd(
            name = mappings.firstOrNull { it.title != null }?.title ?: "${anime.name} - ${StringUtils.toWeeklyEpisodeString(episode, showSeason = false, separator = false)}",
            episodeNumber = numberStr,
            seasonNumber = firstMapping.season,
            description = mappings.firstOrNull { it.description != null }?.description ?: anime.description,
            image = mappings.firstOrNull { it.image != null }?.image ?: firstMapping.uuid?.let { "${Constant.apiUrl}/v1/attachments?uuid=$it&type=BANNER" },
            datePublished = episode.releaseDateTime,
            dateModified = mappings.maxOf { it.lastUpdateDateTime },
            anime = anime,
            duration = mappings.firstOrNull { it.duration > 0 }?.duration,
            potentialActions = mappings.flatMap { mapping ->
                mapping.sources.map { buildPotentialAction(it.url, it.platform, mapping.releaseDateTime, anime.countryCode?.name ?: "FR") }
            }.takeIfNotEmpty()
        )
    }
}
