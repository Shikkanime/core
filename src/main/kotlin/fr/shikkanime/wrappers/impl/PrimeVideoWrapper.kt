package fr.shikkanime.wrappers.impl

import com.google.gson.JsonObject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsLong
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.factories.AbstractPrimeVideoWrapper
import io.ktor.client.statement.*
import io.ktor.http.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object PrimeVideoWrapper : AbstractPrimeVideoWrapper(){
    override suspend fun getEpisodesByShowId(
        countryCode: CountryCode,
        id: String
    ): Array<Episode> {
        // Make API request
        val globalJson = fetchPrimeVideoData("$baseUrl/-/${countryCode.name.lowercase()}/detail/$id/ref=atv_sr_fle_c_sre999aa_1_1_1", countryCode.locale)
        
        // Extract show data
        val atfState = globalJson.getAsJsonObject("atf").getAsJsonObject("state")
        val pageTitleId = atfState.getAsString("pageTitleId")
        val showJson = atfState.getAsJsonObject("detail").getAsJsonObject("headerDetail").getAsJsonObject(pageTitleId)
            ?: throw Exception("Failed to get show")

        // Create show object
        val show = Show(
            id,
            showJson.getAsString("parentTitle") ?: showJson.getAsString("title")!!,
            showJson.getAsJsonObject("images")!!.getAsString("covershot")!!,
            showJson.getAsJsonObject("images")!!.getAsString("heroshot")!!,
            showJson.getAsString("synopsis")
        )

        if (showJson.getAsString("entityType") == "Movie") {
            val audioTracks = showJson.getAsJsonArray("audioTracks")?.map { it.asString }?.toHashSet() ?: HashSet()
            val subtitles = showJson.getAsJsonArray("subtitles")?.map { it.asString }?.toHashSet() ?: HashSet()

            return arrayOf(
                Episode(
                    show,
                    emptySet(),
                    pageTitleId!!.substringAfterLast("."),
                    1,
                    EpisodeType.FILM,
                    1,
                    show.name,
                    show.description,
                    "$baseUrl$id?autoplay=1&t=0",
                    showJson.getAsJsonObject("images")!!.getAsString("covershot")!!,
                    showJson.getAsLong("duration", -1),
                    getAudioLocales(audioTracks),
                    getSubtitleLocales(subtitles),
                )
            )
        }

        // Extract season data
        val seasons = atfState.getAsJsonObject("seasons").getAsJsonArray(pageTitleId)?.map {
            Season(
                it.asJsonObject.getAsString("seasonId")!!,
                it.asJsonObject.getAsString("displayName")!!,
                it.asJsonObject.getAsInt("sequenceNumber")!!,
                it.asJsonObject.getAsString("seasonLink")!!
            )
        }

        // Process all seasons and extract episodes
        return seasons?.flatMap { season ->
            val json = if (season.id != pageTitleId) fetchPrimeVideoData("$baseUrl${season.link}", countryCode.locale) else globalJson
            val btfState = json.getAsJsonObject("btf").getAsJsonObject("state")
            val episodes = mutableSetOf<Episode>()

            // Extract episodes from the current JSON
            btfState.getAsJsonObject("detail").getAsJsonObject("detail")?.let { episodesJson ->
                episodes.addAll(
                    episodesJson.entrySet()
                        .filter { (_, element) -> element.asJsonObject.getAsString("titleType") == "episode" }
                        .map { (key, element) ->
                            createEpisode(key, element.asJsonObject, season, show, btfState.getAsJsonObject("self").getAsJsonObject(key).getAsString("link")!!, btfState)
                        }
                )
            } ?: throw Exception("Failed to get episodes")

            // Handle pagination for additional episodes
            var token = btfState.getAsJsonObject("episodeList")
                ?.getAsJsonObject("actions")
                ?.getAsJsonArray("pagination")
                ?.find { it.asJsonObject.getAsString("tokenType") == "NextPage" }
                ?.asJsonObject?.getAsString("token")

            while (!token.isNullOrEmpty()) {
                val dataObject = loadMoreData(season.id, token)

                token = dataObject.getAsJsonObject("actions")
                    ?.getAsJsonArray("pagination")
                    ?.find { it.asJsonObject.getAsString("tokenType") == "NextPage" }
                    ?.asJsonObject?.getAsString("token")

                dataObject.getAsJsonArray("episodes")?.let { episodesArray ->
                    episodes.addAll(
                        episodesArray.filter { it.asJsonObject?.getAsJsonObject("detail")?.getAsString("titleType") == "episode" }
                            .map {
                                val detailObject = it.asJsonObject!!.getAsJsonObject("detail")!!
                                createEpisode(
                                    detailObject.getAsString("catalogId")!!,
                                    detailObject,
                                    season,
                                    show,
                                    it.asJsonObject.getAsJsonObject("self").getAsString("link")!!,
                                    null
                                )
                            }
                    )
                }
            }

            episodes
        }?.toTypedArray() ?: emptyArray()
    }
    
    private fun createEpisode(
        key: String,
        episodeJson: JsonObject,
        season: Season,
        show: Show,
        link: String,
        btfState: JsonObject?
    ): Episode {
        val episodeNumber = episodeJson.getAsInt("episodeNumber")!!
        val audioTracks = episodeJson.getAsJsonArray("audioTracks")?.map { it.asString }?.toHashSet() ?: HashSet()
        val subtitles = episodeJson.getAsJsonArray("subtitles")?.map { it.asString }?.toHashSet() ?: HashSet()

        return Episode(
            show,
            buildSet {
                add(EncryptionManager.toSHA512("${show.id}-${season.number}-$episodeNumber").substring(0..<8))
                btfState?.let { add(it.getAsJsonObject("self").getAsJsonObject(key).getAsString("compactGTI")!!) }
            },
            key.substringAfterLast("."),
            season.number,
            EpisodeType.EPISODE,
            episodeNumber,
            episodeJson.getAsString("title")!!,
            episodeJson.getAsString("synopsis")!!,
            "$baseUrl$link?autoplay=1&t=0",
            episodeJson.getAsJsonObject("images")!!.getAsString("covershot")!!,
            episodeJson.getAsLong("duration", -1),
            getAudioLocales(audioTracks),
            getSubtitleLocales(subtitles),
        )
    }

    private fun getAudioLocales(audioTracks: HashSet<String>): Set<String> = buildSet {
        if ("日本語" in audioTracks) add("ja-JP")
        if ("日本語" !in audioTracks && "English" in audioTracks) add("en-US")
        if ("Français" in audioTracks) add("fr-FR")
    }

    private fun getSubtitleLocales(subtitles: HashSet<String>): Set<String> = buildSet {
        if ("Français (France)" in subtitles || "Français" in subtitles || "Français [CC]" in subtitles) add("fr-FR")
    }

    private suspend fun fetchPrimeVideoData(url: String, locale: String): JsonObject {
        val response = httpRequest.get(
            "$url?dvWebSPAClientVersion=1.0.111788.0",
            headers = mapOf(
                "Accept" to ContentType.Application.Json.toString(),
                HttpHeaders.Cookie to "lc-main-av=${locale.replace(StringUtils.DASH_STRING, "_")}",
                "x-requested-with" to "WebSPA"
            )
        )
        
        require(response.status == HttpStatusCode.OK) { 
            "Failed to fetch data (${response.status.value} - ${response.bodyAsText()})" 
        }
        
        return ObjectParser.fromJson(response.bodyAsText())
            .getAsJsonArray("page")[0].asJsonObject
            .getAsJsonObject("assembly").getAsJsonArray("body")[0].asJsonObject
            .getAsJsonObject("props") ?: throw Exception("Failed to parse response")
    }

    private suspend fun loadMoreData(id: String, token: String): JsonObject {
        val response = httpRequest.get(
            "$baseUrl/api/getDetailWidgets?titleID=$id&widgets=${URLEncoder.encode("[{\"widgetType\":\"EpisodeList\",\"widgetToken\":\"$token\"}]", StandardCharsets.UTF_8)}",
            headers = mapOf("Accept" to ContentType.Application.Json.toString())
        )

        require(response.status == HttpStatusCode.OK) { "Failed to load more data (${response.status.value} - ${response.bodyAsText()})" }

        return ObjectParser.fromJson(response.bodyAsText())
            .getAsJsonObject("widgets")
            .getAsJsonObject("episodeList") ?: throw Exception("Failed to parse response")
    }
}