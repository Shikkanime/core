package fr.shikkanime.wrappers.impl

import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsBoolean
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsLong
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.normalize
import fr.shikkanime.wrappers.factories.AbstractDisneyPlusWrapper
import io.ktor.client.statement.*
import io.ktor.http.*

object DisneyPlusWrapper : AbstractDisneyPlusWrapper() {
    override suspend fun getShow(id: String): Show {
        val response = httpRequest.getWithAccessToken("${baseUrl}explore/v1.4/page/entity-$id?disableSmartFocus=true&enhancedContainersLimit=15&limit=15")
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

    override suspend fun getEpisodesByShowId(locale: String, showId: String): List<Episode> {
        val show = getShow(showId)
        val episodes = mutableListOf<Episode>()

        show.seasons.forEach { seasonId ->
            var page = 1
            var hasMore: Boolean

            do {
                val response = httpRequest.getWithAccessToken("${baseUrl}explore/v1.4/season/$seasonId?limit=24&offset=${(page++ - 1) * 24}")
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

                        episodes.add(
                            Episode(
                                show,
                                id,
                                it.getAsJsonArray("actions")[0].asJsonObject.getAsJsonObject("legacyPartnerFeed").getAsString("dmcContentId")!!,
                                seasonId,
                                visualsObject.getAsInt("seasonNumber")!!,
                                visualsObject.getAsInt("episodeNumber") ?: -1,
                                visualsObject.getAsString("episodeTitle")?.normalize(),
                                visualsObject.getAsJsonObject("description")?.getAsString("medium")?.normalize(),
                                "https://www.disneyplus.com/${locale.lowercase()}/play/$id",
                                getImageUrl(visualsObject.getAsJsonObject("artwork")!!.getAsJsonObject("standard")!!.getAsJsonObject("thumbnail")!!.getAsJsonObject("1.78")!!.getAsString("imageId")!!),
                                duration
                            )
                        )
                    }

                hasMore = jsonObject.getAsJsonObject("pagination").getAsBoolean("hasMore") == true
            } while (hasMore)
        }

        return episodes
    }

    override suspend fun getShowIdByEpisodeId(episodeId: String): Pair<String, String> {
        val types = listOf("deeplinkId", "dmcContentId")

        val response = types.firstNotNullOfOrNull { type ->
            val response = httpRequest.getWithAccessToken("${baseUrl}explore/v1.7/deeplink?action=playback&refId=$episodeId&refIdType=$type")

            if (response.status == HttpStatusCode.OK) {
                response
            } else {
                null
            }
        }

        requireNotNull(response) { "Failed to fetch show id (${response?.status?.value})" }

        val jsonObject = ObjectParser.fromJson(response.bodyAsText())
            .getAsJsonObject("data")
            .getAsJsonObject("deeplink")
            .getAsJsonArray("actions")[0].asJsonObject

        return jsonObject.getAsString("deeplinkId")!! to jsonObject.getAsJsonObject("partnerFeed")
            .getAsString("evaSeriesEntityId")!!
    }
}