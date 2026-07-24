package fr.shikkanime.wrappers.dtos

import fr.shikkanime.utils.serializers.ZonedDateTimeSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.time.ZonedDateTime

fun List<NetflixMetadataImage>.maxByWidth(): NetflixMetadataImage? =
    this.maxByOrNull { it.width }

fun List<NetflixMetadataImage>.maxByWidthCleanUrl(): String? =
    this.maxByWidth()?.cleanUrl()

@Serializable
data class NetflixResponse<T>(
    val data: T
)

@Serializable
data class NetflixVideos<T>(
    val videos: List<T>
)

@Serializable
data class NetflixMetadataResponse(
    val video: NetflixMetadataVideo,
)

@Serializable
data class NetflixMetadataVideo(
    val seasons: List<NetflixMetadataSeason>,
    val boxart: List<NetflixMetadataImage>,
    val artwork: List<NetflixMetadataImage>,
    val storyart: List<NetflixMetadataImage>
)

@Serializable
data class NetflixMetadataSeason(
    val episodes: List<NetflixMetadataEpisode>,
)

@Serializable
data class NetflixMetadataEpisode(
    val id: Int,
    @SerialName("stills")
    val images: List<NetflixMetadataImage>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NetflixMetadataImage(
    @JsonNames("w", "width")
    val width: Int,
    val url: String
) {
    fun cleanUrl(): String =
        url.substringBefore("?")
}

@Serializable
data class NetflixAvailability(
    val isPlayable: Boolean
)

@Serializable
data class NetflixLatestShowsResponse(
    val jsonGraph: NetflixLatestShowsJsonGraph
) {
    fun latestShows(): List<NetflixLatestShowsItemSummaryValue> =
        jsonGraph.lists.values.asSequence()
            .flatMap { listsByItemId -> listsByItemId.values }
            .flatMap { showList -> showList.itemSummary }
            .mapNotNull { itemSummary -> itemSummary.value }
            .toList()

}

@Serializable
data class NetflixLatestShowsJsonGraph(
    val lists: Map<String, Map<String, NetflixLatestShowsList>>,
)

@Serializable
data class NetflixLatestShowsList(
    val itemSummary: List<NetflixLatestShowsItemSummary>
)

@Serializable
data class NetflixLatestShowsItemSummary(
    val value: NetflixLatestShowsItemSummaryValue? = null
)

@Serializable
data class NetflixLatestShowsItemSummaryValue(
    val id: Int?,
    val title: String?,
    val availability: NetflixAvailability?
)

@Serializable
data class NetflixDetailModalData(
    val unifiedEntities: List<NetflixDetailModalUnifiedEntity>
)

@Serializable
data class NetflixDetailModalUnifiedEntity(
    val title: String,
    val boxartHighRes: NetflixMetadataImage,
    val storyArt: NetflixMetadataImage,
    val logoArtwork: NetflixMetadataImage?,
    val contextualSynopsis: NetflixDetailModalContextualSynopsis?,
    val seasons: NetflixDetailModalSeasons? = null,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val availabilityStartTime: ZonedDateTime?,
    val isAvailable: Boolean,
    val isPlayable: Boolean,
    val genreTags: NetflixGenreTags,
    val runtimeSec: Long? = null,
    val parentShow: NetflixParentShow? = null,
)

@Serializable
data class NetflixDetailModalContextualSynopsis(
    val text: String?
)

@Serializable
data class NetflixDetailModalSeasons(
    val totalCount: Int,
)

@Serializable
data class NetflixGenreTags(
    val edges: List<NetflixEdge<NetflixNode>>
)

@Serializable
data class NetflixEdge<T>(
    val node: T
)

@Serializable
data class NetflixNode(
    val name: String
)

@Serializable
data class NetflixParentShow(
    @SerialName("videoId")
    val id: Int
)

@Serializable
data class NetflixPreviewModalEpisodeSelectorVideo(
    @SerialName("__typename")
    val type: String,
    val seasons: NetflixPreviewModalEpisodeSelectorSeason? = null
)

@Serializable
data class NetflixPreviewModalEpisodeSelectorSeason(
    val edges: List<NetflixEdge<NetflixSeason>>
)

@Serializable
data class NetflixSeason(
    @SerialName("videoId")
    val id: Int,
    val title: String,
    val episodes: NetflixSeasonEpisodes
)

@Serializable
data class NetflixSeasonEpisodes(
    val totalCount: Int
)

@Serializable
data class NetflixPreviewModalEpisodeSelectorSeasonEpisodes(
    val episodes: NetflixPreviewModalEpisodeSelectorSeasonEpisode
)

@Serializable
data class NetflixPreviewModalEpisodeSelectorSeasonEpisode(
    val edges: List<NetflixEdge<NetflixEpisode>>
)

@Serializable
data class NetflixEpisode(
    @SerialName("videoId")
    val id: Int,
    val number: Int,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val availabilityStartTime: ZonedDateTime?,
    val title: String? = null,
    val description: NetflixDetailModalContextualSynopsis? = null,
    val isAvailable: Boolean,
    val isPlayable: Boolean,
    val artwork: NetflixMetadataImage,
    val runtimeSec: Long
)