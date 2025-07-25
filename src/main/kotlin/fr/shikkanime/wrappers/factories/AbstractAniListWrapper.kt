package fr.shikkanime.wrappers.factories

import fr.shikkanime.utils.HttpRequest

abstract class AbstractAniListWrapper {
    data class FuzzyDate(val year: Int)

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
    ) {
        fun maxSimilarity() = listOfNotNull(romajiSearchSimilarity, englishSearchSimilarity, nativeSearchSimilarity).max()
    }

    data class ExternalLink(
        val type: String,
        val site: String,
        val url: String,
    )

    data class RelationEdge(
        val relationType: String,
        val node: Media,
    )

    data class Relations(
        val edges: List<RelationEdge>
    )

    data class Media(
        val id: Int,
        val idMal: Int?,
        val startDate: FuzzyDate,
        val title: Title,
        val format: String?,
        val genres: List<String>?,
        val episodes: Int?,
        val status: Status?,
        val externalLinks: List<ExternalLink>?,
        val relations: Relations?,
        // Calculated
        @Transient
        var hasParentRelation: Boolean = false,
        @Transient
        var isFirstReleasedYearRange: Boolean = false,
    )

    enum class Status {
        RELEASING,
        FINISHED,
        NOT_YET_RELEASED,
        CANCELLED,
        HIATUS
    }

    protected val baseUrl = "https://graphql.anilist.co"
    protected val httpRequest = HttpRequest()

    abstract suspend fun search(query: String, page: Int = 1, limit: Int = 5, status: List<Status> = listOf(Status.RELEASING, Status.FINISHED)): Array<Media>
}