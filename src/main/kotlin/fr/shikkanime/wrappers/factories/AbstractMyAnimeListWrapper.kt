package fr.shikkanime.wrappers.factories

import com.google.gson.annotations.SerializedName
import fr.shikkanime.utils.HttpRequest
import java.io.Serializable
import java.time.ZonedDateTime

abstract class AbstractMyAnimeListWrapper {
    data class Aired(
        val from: ZonedDateTime,
        val to: ZonedDateTime?
    ) : Serializable

    data class Title(
        val type: String,
        val title: String
    ) : Serializable

    data class ExternalLink(
        val name: String,
        val url: String
    ) : Serializable

    data class Media(
        @SerializedName("mal_id")
        val id: Int,
        val aired: Aired,
        val titles: List<Title>,
        val title: String,
        @SerializedName("title_english")
        val titleEnglish: String?,
        @SerializedName("title_japanese")
        val titleJapanese: String?,
        val external: List<ExternalLink>
    ) : Serializable

    data class Episode(
        @SerializedName("mal_id")
        val id: Int,
        val title: String,
        @SerializedName("title_japanese")
        val titleJapanese: String?,
        @SerializedName("title_romanji")
        val titleRomanji: String?,
        val aired: ZonedDateTime
    ) : Serializable

    protected val baseUrl = "https://api.jikan.moe/v4"
    protected val httpRequest = HttpRequest()

    abstract suspend fun getMediaById(id: Int): Media
    abstract suspend fun getEpisodesByMediaId(id: Int): Array<Episode>
}