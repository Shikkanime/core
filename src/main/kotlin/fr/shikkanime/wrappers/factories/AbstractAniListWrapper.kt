package fr.shikkanime.wrappers.factories

import fr.shikkanime.utils.HttpRequest

abstract class AbstractAniListWrapper {
    data class Title(
        val romaji: String,
        val english: String?,
        val native: String?,
    )

    data class Media(
        val id: Int,
        val idMal: Int?,
        val title: Title,
        val format: String?,
        val genres: List<String>,
    )

    protected val baseUrl = "https://graphql.anilist.co"
    protected val httpRequest = HttpRequest()

    abstract suspend fun search(query: String, page: Int = 1, limit: Int = 5): Array<Media>
}