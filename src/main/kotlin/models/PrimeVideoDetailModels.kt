package fr.shikkanime.models

import kotlinx.serialization.Serializable

@Serializable
data class PrimeVideoDetail<T>(
    val body: T
)

@Serializable
data class PrimeVideoDetailBody(
    val atf: PrimeVideoAtf,
    val btf: PrimeVideoBtf
)

@Serializable
data class PrimeVideoDetailWidgets(
    val widgets: PrimeVideoDetailWidgetsWidgets
)

@Serializable
data class PrimeVideoDetailWidgetsWidgets(
    val episodeList: PrimeVideoBtfStateEpisodeList
)