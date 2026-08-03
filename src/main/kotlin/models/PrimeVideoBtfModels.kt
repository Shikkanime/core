package fr.shikkanime.models

import fr.shikkanime.LocalDateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class PrimeVideoBtf(
    val state: PrimeVideoBtfState
)

@Serializable
data class PrimeVideoBtfState(
    val metadata: Map<String, PrimeVideoBtfStateMetadata>,
    val detail: PrimeVideoBtfStateDetail,
    val self: Map<String, PrimeVideoBtfStateSelf>,
    val episodeList: PrimeVideoBtfStateEpisodeList,
)

@Serializable
data class PrimeVideoBtfStateMetadata(
    val traits: List<String>?,
    val upcomingSynopsis: String?,
)

@Serializable
data class PrimeVideoBtfStateDetail(
    val detail: Map<String, PrimeVideoBtfStateDetailDetail>,
)

@Serializable
enum class PrimeVideoBtfStateDetailDetailTitleType {
    @SerialName("episode")
    EPISODE,
    @SerialName("season")
    SEASON
}

@Serializable
data class PrimeVideoBtfStateDetailDetailImages(
    val covershot: String
)

@Serializable
data class PrimeVideoBtfStateDetailDetail(
    val catalogId: String?,
    val titleType: PrimeVideoBtfStateDetailDetailTitleType,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate,
    val episodeNumber: Int?,
    val audioTracks: Set<String>,
    val subtitles: Set<String>,
    val title: String,
    val synopsis: String,
    val images: PrimeVideoBtfStateDetailDetailImages,
    val duration: Long?
)

@Serializable
data class PrimeVideoBtfStateSelf(
    val link: String,
)

@Serializable
data class PrimeVideoBtfStateEpisodeList(
    val actions: PrimeVideoBtfStateEpisodeListActions,
    val episodes: List<PrimeVideoBtfStateEpisodeListEpisode>?,
) {
    val nextPageToken: String?
        get() = actions.pagination.firstOrNull { it.tokenType == PrimeVideoBtfStateEpisodeListActionsPaginationTokenType.NEXT_PAGE }
            ?.token
}

@Serializable
data class PrimeVideoBtfStateEpisodeListActions(
    val pagination: List<PrimeVideoBtfStateEpisodeListActionsPagination>
)

@Serializable
enum class PrimeVideoBtfStateEpisodeListActionsPaginationTokenType {
    @SerialName("NextPage")
    NEXT_PAGE,
    @SerialName("PrevPage")
    PREV_PAGE,
}

@Serializable
data class PrimeVideoBtfStateEpisodeListActionsPagination(
    val tokenType: PrimeVideoBtfStateEpisodeListActionsPaginationTokenType,
    val token: String
)

@Serializable
data class PrimeVideoBtfStateEpisodeListEpisode(
    val detail: PrimeVideoBtfStateDetailDetail,
    val self: PrimeVideoBtfStateSelf,
)