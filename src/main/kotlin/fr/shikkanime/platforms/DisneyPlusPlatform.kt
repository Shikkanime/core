package fr.shikkanime.platforms

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import fr.shikkanime.caches.CountryCodeDisneyPlusSimulcastKeyCache
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.exceptions.AnimeException
import fr.shikkanime.platforms.configuration.DisneyPlusConfiguration
import fr.shikkanime.utils.*
import fr.shikkanime.utils.ObjectParser.getAsBoolean
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsLong
import fr.shikkanime.utils.ObjectParser.getAsString
import io.ktor.client.statement.*
import java.io.File
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level

class DisneyPlusPlatform :
    AbstractPlatform<DisneyPlusConfiguration, CountryCodeDisneyPlusSimulcastKeyCache, JsonArray>() {
    override fun getPlatform(): Platform = Platform.DISN

    override fun getConfigurationClass() = DisneyPlusConfiguration::class.java

    override suspend fun fetchApiContent(
        key: CountryCodeDisneyPlusSimulcastKeyCache,
        zonedDateTime: ZonedDateTime
    ): JsonArray {
        check(configuration!!.authorization.isNotBlank()) { "Authorization is null" }
        check(configuration!!.refreshToken.isNotBlank()) { "Refresh token is null" }
        val httpRequest = HttpRequest()

        val loginDevice = httpRequest.post(
            "https://disney.api.edge.bamgrid.com/graph/v1/device/graphql",
            headers = mapOf(
                "Authorization" to configuration!!.authorization,
            ),
            body = ObjectParser.toJson(
                mapOf(
                    "operationName" to "refreshToken",
                    "query" to "mutation refreshToken(\$input:RefreshTokenInput!){refreshToken(refreshToken:\$input){activeSession{sessionId}}}",
                    "variables" to mapOf(
                        "input" to mapOf(
                            "refreshToken" to configuration!!.refreshToken
                        )
                    ),
                )
            )
        )

        check(loginDevice.status.value == 200) { "Failed to login to Disney+" }
        val loginDeviceJson = ObjectParser.fromJson(loginDevice.bodyAsText(), JsonObject::class.java)
        val accessToken = loginDeviceJson.getAsJsonObject("extensions").getAsJsonObject("sdk").getAsJsonObject("token")
            .getAsString("accessToken")

        val seasonsResponse = httpRequest.get(
            "https://disney.content.edge.bamgrid.com/svc/content/DmcSeriesBundle/version/5.1/region/${key.countryCode.name}/audience/k-false,l-true/maturity/1850/language/${key.countryCode.locale}/encodedSeriesId/${key.disneyPlusSimulcast.name}",
            mapOf("Authorization" to "Bearer $accessToken")
        )
        check(seasonsResponse.status.value == 200) { "Failed to fetch Disney+ content" }
        val seasonsJson = ObjectParser.fromJson(seasonsResponse.bodyAsText(), JsonObject::class.java)
        val seasons = seasonsJson.getAsJsonObject("data").getAsJsonObject("DmcSeriesBundle").getAsJsonObject("seasons")
            .getAsJsonArray("seasons").mapNotNull { it.asJsonObject.getAsString("seasonId") }
        val episodes = JsonArray()

        seasons.forEach { season ->
            var page = 1
            var hasMore: Boolean

            do {
                val url =
                    "https://disney.content.edge.bamgrid.com/svc/content/DmcEpisodes/version/5.1/region/${key.countryCode.name}/audience/k-false,l-true/maturity/1850/language/${key.countryCode.locale}/seasonId/${season}/pageSize/15/page/${page++}"
                val response = httpRequest.get(url, mapOf("Authorization" to "Bearer $accessToken"))
                check(response.status.value == 200) { "Failed to fetch Disney+ content" }
                val json = ObjectParser.fromJson(response.bodyAsText(), JsonObject::class.java)

                val dmcEpisodesMeta = json.getAsJsonObject("data").getAsJsonObject("DmcEpisodes")
                hasMore = dmcEpisodesMeta.getAsJsonObject("meta").getAsBoolean("hasMore") ?: false
                dmcEpisodesMeta.getAsJsonArray("videos").forEach { episodes.add(it) }
            } while (hasMore)
        }

        return episodes
    }

    override fun fetchEpisodes(zonedDateTime: ZonedDateTime, bypassFileContent: File?): List<Episode> {
        val list = mutableListOf<Episode>()

        configuration!!.availableCountries.forEach { countryCode ->
            configuration!!.simulcasts.filter {
                it.releaseDay == zonedDateTime.dayOfWeek.value && zonedDateTime.toLocalTime()
                    .isEqualOrAfter(LocalTime.parse(it.releaseTime))
            }.forEach { simulcast ->
                val api = getApiContent(CountryCodeDisneyPlusSimulcastKeyCache(countryCode, simulcast), zonedDateTime)

                api.forEach {
                    try {
                        list.add(convertEpisode(countryCode, simulcast, it.asJsonObject, zonedDateTime))
                    } catch (_: AnimeException) {
                        // Ignore
                    } catch (e: Exception) {
                        logger.log(Level.SEVERE, "Error on converting episode", e)
                    }
                }
            }
        }

        return list
    }

    private fun convertEpisode(
        countryCode: CountryCode,
        simulcast: DisneyPlusConfiguration.DisneyPlusSimulcast,
        jsonObject: JsonObject,
        zonedDateTime: ZonedDateTime
    ): Episode {
        val texts = jsonObject.getAsJsonObject("text")
        val titles = texts?.getAsJsonObject("title")?.getAsJsonObject("full")
        val descriptions = texts.getAsJsonObject("description")?.getAsJsonObject("medium")
        val animeName = titles?.getAsJsonObject("series")?.getAsJsonObject("default")?.getAsString("content")
            ?: throw Exception("Anime name is null")

        val animeImage = jsonObject.getAsJsonObject("image")?.getAsJsonObject("tile")?.getAsJsonObject("0.71")
            ?.getAsJsonObject("series")?.getAsJsonObject("default")?.getAsString("url")
            ?: throw Exception("Anime image is null")
        val animeBanner = jsonObject.getAsJsonObject("image")?.getAsJsonObject("tile")?.getAsJsonObject("1.33")
            ?.getAsJsonObject("series")?.getAsJsonObject("default")?.getAsString("url")
            ?: throw Exception("Anime image is null")
        val animeDescription = descriptions?.getAsJsonObject("series")
            ?.getAsJsonObject("default")?.getAsString("content")?.replace('\n', ' ') ?: ""

        val season = jsonObject.getAsInt("seasonSequenceNumber")

        val number = jsonObject.getAsInt("episodeSequenceNumber")

        val id = requireNotNull(jsonObject.getAsString("contentId")) { "Id is null" }

        val title =
            titles.getAsJsonObject("program")?.getAsJsonObject("default")?.getAsString("content")?.ifBlank { null }

        val url = "https://www.disneyplus.com/${countryCode.locale.lowercase()}/video/$id"

        val image = jsonObject.getAsJsonObject("image")?.getAsJsonObject("thumbnail")?.getAsJsonObject("1.78")
            ?.getAsJsonObject("program")?.getAsJsonObject("default")?.getAsString("url")
            ?: throw Exception("Image is null")

        var duration = jsonObject.getAsJsonObject("mediaMetadata")?.getAsLong("runtimeMillis", -1) ?: -1

        if (duration != -1L) {
            duration /= 1000
        }

        val description = descriptions?.getAsJsonObject("program")
            ?.getAsJsonObject("default")?.getAsString("content")?.replace('\n', ' ')?.ifBlank { null }

        val langType = LangType.SUBTITLES
        val releaseDateTimeUTC = zonedDateTime.withUTC()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T${simulcast.releaseTime}Z"
        val releaseDateTime = ZonedDateTime.parse(releaseDateTimeUTC)
        val audioLocale = "ja-JP"
        val (_, hash) = getDeprecatedHashAndHash(countryCode, id, audioLocale, langType)

        return Episode(
            platform = getPlatform(),
            anime = Anime(
                countryCode = countryCode,
                name = animeName,
                releaseDateTime = releaseDateTime,
                image = animeImage,
                banner = animeBanner,
                description = animeDescription,
                slug = StringUtils.toSlug(StringUtils.getShortName(animeName)),
            ),
            episodeType = EpisodeType.EPISODE,
            langType = langType,
            audioLocale = audioLocale,
            hash = hash,
            releaseDateTime = releaseDateTime,
            season = season,
            number = number,
            title = title,
            url = url,
            image = image,
            duration = duration,
            description = description,
        )
    }
}