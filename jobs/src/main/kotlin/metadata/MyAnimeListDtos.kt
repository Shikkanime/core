package fr.shikkanime.jobs.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MyAnimeListMediaResponse(
    val data: MyAnimeListMedia?
)

@Serializable
data class MyAnimeListSearchResponse(
    val data: List<MyAnimeListMedia> = emptyList()
)

@Serializable
data class MyAnimeListPagination(
    @SerialName("has_next_page")
    val hasNextPage: Boolean
)

@Serializable
data class MyAnimeListEpisodeResponse(
    val pagination: MyAnimeListPagination,
    val data: List<MyAnimeListEpisode>
)

@Serializable
data class MyAnimeListTitle(
    val type: String,
    val title: String
)

@Serializable
data class MyAnimeListExternalLink(
    val name: String,
    val url: String? = null
)

// The API emits partial dates ("2003", "2003-05") as well as full timestamps: kept as raw
// strings and parsed leniently by the client.
@Serializable
data class MyAnimeListAiring(
    val from: String? = null,
    val to: String? = null
)

@Serializable
data class MyAnimeListMedia(
    @SerialName("mal_id")
    val id: Int,
    val titles: List<MyAnimeListTitle>,
    val synopsis: String? = null,
    val type: String? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val aired: MyAnimeListAiring? = null,
    @SerialName("external")
    val externalLinks: List<MyAnimeListExternalLink> = emptyList(),
    val genres: List<MyAnimeListEntry> = emptyList(),
    val themes: List<MyAnimeListEntry> = emptyList()
)

@Serializable
data class MyAnimeListEntry(
    val name: String
)

@Serializable
data class MyAnimeListEpisode(
    @SerialName("mal_id")
    val id: Int,
    val title: String? = null,
    @SerialName("title_japanese")
    val titleJapanese: String? = null,
    @SerialName("title_romanji")
    val titleRomanji: String? = null,
    val aired: String? = null
)
