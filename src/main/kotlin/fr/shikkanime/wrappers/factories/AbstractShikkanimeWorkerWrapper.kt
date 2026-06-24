package fr.shikkanime.wrappers.factories

import kotlinx.serialization.Serializable

abstract class AbstractShikkanimeWorkerWrapper {
    @Serializable
    data class Request(
        val ids: List<Int>,
        val netflixId: String,
        val secureNetflixId: String
    )

    @Serializable
    data class Episode(
        val id: Int,
        val audioLocales: List<String>
    )

    protected val baseUrl = "https://worker.shikkanime.fr"

    abstract suspend fun getNetflixEpisodes(netflixId: String, secureNetflixId: String, vararg ids: Int): List<Episode>
}