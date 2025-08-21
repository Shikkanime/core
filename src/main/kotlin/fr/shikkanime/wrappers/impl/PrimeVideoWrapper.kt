package fr.shikkanime.wrappers.impl

import com.google.gson.JsonObject
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsLong
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.factories.AbstractPrimeVideoWrapper
import io.ktor.client.statement.*
import io.ktor.http.*

object PrimeVideoWrapper : AbstractPrimeVideoWrapper(){
    override suspend fun getEpisodesByShowId(
        locale: String,
        id: String
    ): Array<Episode> {
        // Make API request
        val globalJson = fetchPrimeVideoData("$baseUrl/detail/$id/ref=atv_sr_fle_c_Tn74RA_1_1_1", locale)
        
        // Extract show data
        val atfState = globalJson.getAsJsonObject("atf").getAsJsonObject("state")
        val pageTitleId = atfState.getAsString("pageTitleId")
        val showJson = atfState.getAsJsonObject("detail").getAsJsonObject("headerDetail").getAsJsonObject(pageTitleId)
            ?: throw Exception("Failed to get show")

        // Create show object
        val show = Show(
            id,
            showJson.getAsString("parentTitle")!!,
            showJson.getAsJsonObject("images")!!.getAsString("covershot")!!,
            showJson.getAsJsonObject("images")!!.getAsString("heroshot")!!,
            showJson.getAsString("synopsis")
        )

        // Extract season data
        val seasons = atfState.getAsJsonObject("seasons").getAsJsonArray(pageTitleId).map {
            Season(
                it.asJsonObject.getAsString("seasonId")!!,
                it.asJsonObject.getAsString("displayName")!!,
                it.asJsonObject.getAsInt("sequenceNumber")!!,
                it.asJsonObject.getAsString("seasonLink")!!
            )
        }

        // Process all seasons and extract episodes
        return seasons.flatMap { season ->
            // Use existing JSON for main page or fetch season-specific data
            val json = if (season.id != pageTitleId) {
                fetchPrimeVideoData("$baseUrl${season.link}", locale)
            } else {
                globalJson
            }

            // Extract episodes data
            val btfState = json.getAsJsonObject("btf").getAsJsonObject("state")
            val episodesJson = btfState.getAsJsonObject("detail").getAsJsonObject("detail")
                ?: throw Exception("Failed to get episodes")

            // Map JSON to Episode objects
            episodesJson.entrySet().asSequence()
                .filter { (_, element) -> element.asJsonObject.getAsString("titleType") == "episode" }
                .map { (key, element) -> 
                    createEpisode(key, element.asJsonObject, season, show, btfState)
                }
        }.toTypedArray()
    }
    
    private fun createEpisode(
        key: String,
        episodeJson: JsonObject,
        season: Season,
        show: Show,
        btfState: JsonObject
    ): Episode {
        val episodeNumber = episodeJson.getAsInt("episodeNumber")!!
        val audioTracks = episodeJson.getAsJsonArray("audioTracks")?.map { it.asString }?.toHashSet() ?: HashSet()
        val subtitles = episodeJson.getAsJsonArray("subtitles")?.map { it.asString }?.toHashSet() ?: HashSet()

        return Episode(
            show,
            setOf(
                EncryptionManager.toSHA512("${show.id}-${season.number}-$episodeNumber").substring(0..<8),
                btfState.getAsJsonObject("self").getAsJsonObject(key).getAsString("compactGTI")!!
            ),
            key.substringAfterLast("."),
            season.number,
            episodeNumber,
            episodeJson.getAsString("title")!!,
            episodeJson.getAsString("synopsis")!!,
            "$baseUrl${btfState.getAsJsonObject("self").getAsJsonObject(key).getAsString("link")}",
            episodeJson.getAsJsonObject("images")!!.getAsString("covershot")!!,
            episodeJson.getAsLong("duration", -1),
            buildSet {
                if ("日本語" in audioTracks) add("ja-JP")
                if ("日本語" !in audioTracks && "English" in audioTracks) add("en-US")
                if ("Français" in audioTracks) add("fr-FR")
            },
            buildSet {
                if ("Français (France)" in subtitles || "Français" in subtitles) add("fr-FR")
            },
        )
    }
    
    private suspend fun fetchPrimeVideoData(url: String, locale: String): JsonObject {
        val response = httpRequest.get(
            "$url?dvWebSPAClientVersion=1.0.105438.0",
            headers = mapOf(
                "Accept" to "application/json",
                "Cookie" to "lc-main-av=${locale.replace(StringUtils.DASH_STRING, "_")}",
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
}