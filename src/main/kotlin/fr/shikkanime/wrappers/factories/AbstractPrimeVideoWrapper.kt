package fr.shikkanime.wrappers.factories

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.utils.HttpRequest
import java.io.Serializable

abstract class AbstractPrimeVideoWrapper {
    data class Show(
        val id: String,
        val name: String,
        val banner: String,
        val carousel: String,
        val description: String?,
    ) : Serializable

    data class Season(
        val id: String,
        val name: String,
        val number: Int,
        val link: String
    )

    data class Episode(
        val show: Show,
        val oldIds: Set<String>,
        val id: String,
        val season: Int,
        val episodeType: EpisodeType,
        val number: Int,
        val title: String?,
        val description: String?,
        val url: String,
        val image: String,
        val duration: Long,
        val audioLocales: Set<String>,
        val subtitleLocales: Set<String>,
    ) : Serializable

    protected val baseUrl = "https://www.primevideo.com"
    protected val httpRequest = HttpRequest()

    abstract suspend fun getEpisodesByShowId(countryCode: CountryCode, id: String): Array<Episode>
}