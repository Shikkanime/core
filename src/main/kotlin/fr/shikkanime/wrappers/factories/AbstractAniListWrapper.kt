package fr.shikkanime.wrappers.factories

import fr.shikkanime.utils.HttpRequest
import java.io.Serializable

abstract class AbstractAniListWrapper {
    data class FuzzyDate(val year: Int) : Serializable

    data class Title(
        val romaji: String,
        val english: String?,
        val native: String?,
        // Calculated
        @Transient
        var romajiSearchSimilarity: Double?,
        @Transient
        var englishSearchSimilarity: Double?,
        @Transient
        var nativeSearchSimilarity: Double?,
    ) : Serializable {
        fun maxSimilarity() = listOfNotNull(romajiSearchSimilarity, englishSearchSimilarity, nativeSearchSimilarity).max()
    }

    data class ExternalLink(
        val type: String,
        val site: String,
        val url: String,
    ) : Serializable

    data class RelationEdge(
        val relationType: String,
        val node: Media,
    ) : Serializable

    data class Relations(
        val edges: List<RelationEdge>
    ) : Serializable

    data class Tag(
        val name: String,
        val isAdult: Boolean,
        val isGeneralSpoiler: Boolean,
        val isMediaSpoiler: Boolean,
        val rank: Int,
    ) : Serializable

    data class Media(
        val id: Int,
        val idMal: Int?,
        val startDate: FuzzyDate,
        val title: Title,
        val synonyms: List<String>?,
        val format: String?,
        val genres: List<String>?,
        val tags: List<Tag>?,
        val episodes: Int?,
        val status: Status?,
        val externalLinks: List<ExternalLink>?,
        val relations: Relations?,
        val type: String?,
        // Calculated
        @Transient
        var hasParentRelation: Boolean = false,
        @Transient
        var isFirstReleasedYearRange: Boolean = false,
    ) : Serializable

    enum class Status : Serializable {
        RELEASING,
        FINISHED,
        NOT_YET_RELEASED,
        CANCELLED,
        HIATUS
    }

    protected val baseUrl = "https://graphql.anilist.co"
    protected val httpRequest = HttpRequest()

    abstract suspend fun search(query: String, page: Int = 1, limit: Int = 5, status: List<Status> = listOf(Status.RELEASING, Status.FINISHED)): Array<Media>
    abstract suspend fun getMediaById(id: Int): Media
}