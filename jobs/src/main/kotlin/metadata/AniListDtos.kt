package fr.shikkanime.jobs.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AniListRequest(
    val query: String,
    val variables: AniListVariables
)

@Serializable
data class AniListVariables(
    val search: String? = null,
    val idIn: List<Int>? = null,
    val page: Int? = null,
    val perPage: Int? = null
)

@Serializable
data class AniListPageResponse(
    val data: AniListPageData?
)

@Serializable
data class AniListPageData(
    @SerialName("Page")
    val page: AniListPage?
)

@Serializable
data class AniListPage(
    val media: List<AniListMedia>?
)

@Serializable
data class AniListMedia(
    val id: Int,
    val idMal: Int? = null,
    val format: String? = null,
    val title: AniListTitle? = null,
    val synonyms: List<String>? = null,
    val description: String? = null,
    val episodes: Int? = null,
    val duration: Int? = null,
    val startDate: AniListDate? = null,
    val externalLinks: List<AniListExternalLink>? = null,
    val relations: AniListRelations? = null
)

@Serializable
data class AniListTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
)

@Serializable
data class AniListDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)

@Serializable
data class AniListExternalLink(
    val site: String? = null,
    val url: String? = null
)

@Serializable
data class AniListRelations(
    val edges: List<AniListRelationEdge>? = null
)

@Serializable
data class AniListRelationEdge(
    val relationType: String? = null,
    val node: AniListRelationNode? = null
)

@Serializable
data class AniListRelationNode(
    val id: Int,
    val type: String? = null,
    val format: String? = null,
    val title: AniListTitle? = null
)
