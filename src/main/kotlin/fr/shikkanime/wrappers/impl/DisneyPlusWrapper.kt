package fr.shikkanime.wrappers.impl

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsBoolean
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsLong
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.normalize
import fr.shikkanime.wrappers.factories.AbstractDisneyPlusWrapper
import io.ktor.client.statement.*
import io.ktor.http.*
import java.util.*

object DisneyPlusWrapper : AbstractDisneyPlusWrapper() {
    override suspend fun getShow(id: String): Show {
        val response = httpRequest.getWithAccessToken("${baseUrl}explore/v1.10/page/entity-$id?disableSmartFocus=true&enhancedContainersLimit=15&limit=15")
        require(response.status == HttpStatusCode.OK) { "Failed to fetch show (${response.status.value})" }
        val jsonObject = ObjectParser.fromJson(response.bodyAsText())
            .getAsJsonObject("data")
            .getAsJsonObject("page")

        val seasons = jsonObject.getAsJsonArray("containers")
            .filter { it.asJsonObject.getAsString("type") == "episodes" }
            .map { it.asJsonObject }
            .getOrNull(0)
            ?.getAsJsonArray("seasons")
            ?.mapNotNull { it.asJsonObject.getAsString("id") }
            ?.toSet() ?: emptySet()

        val showObject = jsonObject.getAsJsonObject("visuals")
        val tileObject = showObject.getAsJsonObject("artwork")!!.getAsJsonObject("standard")!!.getAsJsonObject("tile")!!

        return Show(
            id = id,
            name = showObject.getAsString("title")!!,
            image = getImageUrl(tileObject.getAsJsonObject("0.71")!!.getAsString("imageId")!!),
            banner = getImageUrl(tileObject.getAsJsonObject("1.33")!!.getAsString("imageId")!!),
            description = showObject.getAsJsonObject("description")?.getAsString("full"),
            seasons = seasons
        )
    }

    override suspend fun getEpisodesByShowId(locale: String, showId: String, checkAudioLocales: Boolean): List<Episode> {
        val show = getShow(showId)
        val episodes = mutableListOf<Episode>()

        show.seasons.forEach { seasonId ->
            var page = 1
            var hasMore: Boolean

            do {
                val response = httpRequest.getWithAccessToken("${baseUrl}explore/v1.10/season/$seasonId?limit=24&offset=${(page++ - 1) * 24}")
                require(response.status == HttpStatusCode.OK) { "Failed to fetch episodes (${response.status.value})" }

                val jsonObject = ObjectParser.fromJson(response.bodyAsText())
                    .getAsJsonObject("data")
                    .getAsJsonObject("season")

                jsonObject.getAsJsonArray("items").asSequence()
                    .map { it.asJsonObject }
                    .filter { it.getAsString("type") == "view" }
                    .forEach {
                        val visualsObject = it.getAsJsonObject("visuals")
                        val id = it.getAsString("id")!!

                        var duration = visualsObject.getAsLong("durationMs", -1)

                        if (duration != -1L) {
                            duration /= 1000
                        }

                        val actionJsonObject = it.getAsJsonArray("actions")[0].asJsonObject
                        val resourceId = actionJsonObject.getAsString("resourceId")!!
                        val audioLocales = if (checkAudioLocales) getAudioLocales(resourceId) else setOf("ja-JP")

                        episodes.add(
                            Episode(
                                show,
                                id,
                                actionJsonObject.getAsJsonObject("legacyPartnerFeed").getAsString("dmcContentId")!!,
                                seasonId,
                                visualsObject.getAsInt("seasonNumber")!!,
                                visualsObject.getAsInt("episodeNumber") ?: -1,
                                visualsObject.getAsString("episodeTitle")?.normalize(),
                                visualsObject.getAsJsonObject("description")?.getAsString("medium")?.normalize(),
                                "https://www.disneyplus.com/${locale.lowercase()}/play/$id",
                                getImageUrl(visualsObject.getAsJsonObject("artwork")!!.getAsJsonObject("standard")!!.getAsJsonObject("thumbnail")!!.getAsJsonObject("1.78")!!.getAsString("imageId")!!),
                                duration,
                                resourceId,
                                audioLocales
                            )
                        )
                    }

                hasMore = jsonObject.getAsJsonObject("pagination").getAsBoolean("hasMore") == true
            } while (hasMore)
        }

        return episodes
    }

    override suspend fun getAudioLocales(resourceId: String): Set<String> {
        val headers = mapOf(
            "x-application-version" to "1.1.2",
            "x-bamsdk-client-id" to "disney-svod-3d9324fc",
            "x-bamsdk-platform" to "javascript/windows/chrome",
            "x-bamsdk-version" to "31.1",
            "x-dss-edge-accept" to "vnd.dss.edge+json; version=2",
            "x-dss-feature-filtering" to "true"
        )
        val body = """{
            "playback": {
                "attributes": {
                    "resolution": {"max": ["1280x720"]},
                    "protocol": "HTTPS",
                    "assetInsertionStrategies": {"point": "SGAI", "range": "SGAI"},
                    "playbackInitiationContext": "ONLINE",
                    "frameRates": [30]
                },
                "adTracking": {
                    "limitAdTrackingEnabled": "NOT_SUPPORTED",
                    "deviceAdId": "00000000-0000-0000-0000-000000000000"
                },
                "tracking": {
                    "playbackSessionId": "${UUID.randomUUID()}"
                }
            },
            "playbackId": "$resourceId"
        }""".trimIndent()

        val response = httpRequest.postWithAccessToken(
            "${baseUrl}v7/playback/ctr-regular",
            headers,
            body
        )
        require(response.status == HttpStatusCode.OK) {
            "Failed to fetch video data (${response.status.value} - ${response.bodyAsText()})"
        }

        val jsonObject = ObjectParser.fromJson(response.bodyAsText())
            .getAsJsonObject("stream")
            .getAsJsonObject("renditions")

        val supportedLanguages = CountryCode.entries.map { it.locale }

        val subtitleLocales = jsonObject.getAsJsonArray("subtitles")
            .mapNotNull { it.asJsonObject.getAsString("language") }
            .toSet()

        val audioLocales = jsonObject.getAsJsonArray("audio")
            .mapNotNull { it.asJsonObject.getAsString("language") }
            .toSet()

        if (subtitleLocales.none { it in supportedLanguages }) return emptySet()

        return audioLocales
            .map { if (it == "ja") "ja-JP" else it }
            .filter { it == "ja-JP" || it in supportedLanguages }
            .toSet()
    }
}