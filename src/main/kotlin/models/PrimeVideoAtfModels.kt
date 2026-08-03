package fr.shikkanime.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrimeVideoAtf(
    val state: PrimeVideoAtfState
)

@Serializable
data class PrimeVideoAtfState(
    val pageTitleId: String,
    val detail: PrimeVideoAtfStateDetail,
    val seasons: Map<String, List<PrimeVideoAtfStateSeason>>
)

@Serializable
data class PrimeVideoAtfStateDetail(
    val headerDetail: Map<String, PrimeVideoAtfStateDetailHeaderDetail>
)

@Serializable
enum class PrimeVideoAtfStateDetailHeaderDetailEntityType {
    @SerialName("Movie")
    MOVIE,
    @SerialName("TV Show")
    TV_SHOW
}

@Serializable
data class PrimeVideoAtfStateDetailHeaderDetail(
    val entityType: PrimeVideoAtfStateDetailHeaderDetailEntityType,
    val parentTitle: String,
    val synopsis: String?,
    val images: PrimeVideoAtfStateDetailHeaderDetailImages,
    val audioTracks: Set<String>,
    val subtitles: Set<String>,
    val duration: Long?
)

@Serializable
data class PrimeVideoAtfStateDetailHeaderDetailImages(
    val covershot: String,
    val heroshot: String,
    val titleLogo: String
)

@Serializable
data class PrimeVideoAtfStateSeason(
    val seasonId: String,
    val displayName: String,
    val sequenceNumber: Int,
    val seasonLink: String
)