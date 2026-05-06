package fr.shikkanime.wrappers.factories

import com.google.gson.JsonObject
import fr.shikkanime.entities.enums.EpisodeType
import java.io.Serializable

abstract class AbstractPrimeVideoWrapper : IStreamingPlatformWrapper<String, AbstractPrimeVideoWrapper.Show, AbstractPrimeVideoWrapper.Episode> {
    data class Show(
        val globalJson: JsonObject,
        val atfState: JsonObject,
        override val id: String,
        val name: String,
        val banner: String,
        val carousel: String,
        val title: String,
        val description: String?,
    ) : Serializable, IStreamingPlatformWrapper.Id<String>

    data class Season(
        val id: String,
        val name: String,
        val number: Int,
        val link: String
    )

    data class Episode(
        val show: Show,
        val oldIds: Set<String>,
        override val id: String,
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
    ) : Serializable, IStreamingPlatformWrapper.Id<String>

    protected val baseUrl = "https://www.primevideo.com"
}