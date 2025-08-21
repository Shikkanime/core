package fr.shikkanime.wrappers.impl

import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.factories.AbstractCrunchyrollWrapper
import io.ktor.client.statement.*
import io.ktor.http.*

object CrunchyrollWrapper : AbstractCrunchyrollWrapper() {
    override suspend fun getBrowse(
        locale: String,
        sortBy: SortType,
        type: MediaType,
        size: Int,
        start: Int,
        simulcast: String?
    ): List<BrowseObject> {
        val response = httpRequest.getWithAccessToken("${baseUrl}content/v2/discover/browse?sort_by=${sortBy.name.lowercase()}&type=${type.name.lowercase()}&n=$size&start=$start&locale=$locale${if (simulcast != null) "&seasonal_tag=$simulcast" else StringUtils.EMPTY_STRING}")
        require(response.status == HttpStatusCode.OK) { "Failed to get media list (${response.status.value})" }
        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data") ?: throw Exception("Failed to get media list")
        return ObjectParser.fromJson(asJsonArray, Array<BrowseObject>::class.java).toList()
    }

    override suspend fun getSeries(
        locale: String,
        id: String
    ): Series {
         val response = httpRequest.getWithAccessToken("${baseUrl}content/v2/cms/series/$id?locale=$locale")
        require(response.status == HttpStatusCode.OK) { "Failed to get series (${response.status.value})" }
        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data") ?: throw Exception("Failed to get series")
        return ObjectParser.fromJson(asJsonArray.first(), Series::class.java)
    }

    override suspend fun getSeasonsBySeriesId(
        locale: String,
        id: String
    ): Array<Season> {
        val response = httpRequest.getWithAccessToken("${baseUrl}content/v2/cms/series/$id/seasons?locale=$locale")
        require(response.status == HttpStatusCode.OK) { "Failed to get seasons with series id (${response.status.value})" }
        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data") ?: throw Exception("Failed to get seasons with series id")
        return ObjectParser.fromJson(asJsonArray, Array<Season>::class.java)
    }

    override suspend fun getSeason(
        locale: String,
        id: String
    ): Season {
        val response = httpRequest.getWithAccessToken("${baseUrl}content/v2/cms/seasons/$id?locale=$locale")
        require(response.status == HttpStatusCode.OK) { "Failed to get season (${response.status.value})" }
        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data") ?: throw Exception("Failed to get season")
        return ObjectParser.fromJson(asJsonArray.first(), Season::class.java)
    
    }

    override suspend fun getEpisodesBySeasonId(
        locale: String,
        id: String
    ): Array<Episode> {
        val response = httpRequest.getWithAccessToken("${baseUrl}content/v2/cms/seasons/$id/episodes?locale=$locale")
        require(response.status == HttpStatusCode.OK) { "Failed to get episodes by season id (${response.status.value})" }
        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data") ?: throw Exception("Failed to get episodes by season id")
        return ObjectParser.fromJson(asJsonArray, Array<Episode>::class.java)
    }

    @JvmStatic
    suspend fun getJvmStaticEpisodesBySeasonId(locale: String, id: String) = getEpisodesBySeasonId(locale, id)

    override suspend fun getEpisode(
        locale: String,
        id: String
    ): Episode {
        val response = httpRequest.getWithAccessToken("${baseUrl}content/v2/cms/episodes/$id?locale=$locale")
        require(response.status == HttpStatusCode.OK) { "Failed to get episode (${response.status.value})" }
        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data") ?: throw Exception("Failed to get episode")
        return ObjectParser.fromJson(asJsonArray.first(), Episode::class.java)
    }

    @JvmStatic
    suspend fun getJvmStaticEpisode(locale: String, id: String) = getEpisode(locale, id)

    override suspend fun getEpisodeDiscoverByType(
        locale: String,
        type: String,
        id: String
    ): BrowseObject {
        val response = httpRequest.getWithAccessToken("${baseUrl}content/v2/discover/$type/$id?locale=$locale")
        require(response.status == HttpStatusCode.OK) { "Failed to get $type episode (${response.status.value})" }
        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data") ?: throw Exception("Failed to get $type episode")
        return ObjectParser.fromJson(asJsonArray.first().asJsonObject["panel"].asJsonObject, BrowseObject::class.java)
    }

    @JvmStatic
    suspend fun getUpNext(locale: String, id: String) = getEpisodeDiscoverByType(locale, "up_next", id)

    override suspend fun getObjects(
        locale: String,
        vararg ids: String
    ): List<BrowseObject> {
        val response = httpRequest.getWithAccessToken("${baseUrl}content/v2/cms/objects/${ids.joinToString(StringUtils.COMMA_STRING)}?locale=$locale")
        require(response.status == HttpStatusCode.OK) { "Failed to get objects (${response.status.value})" }
        val asJsonArray = ObjectParser.fromJson(response.bodyAsText()).getAsJsonArray("data") ?: throw Exception("Failed to get objects")
        return ObjectParser.fromJson(asJsonArray, Array<BrowseObject>::class.java).toList()
    }

    @JvmStatic
    suspend fun getJvmStaticObjects(locale: String, vararg ids: String) = getObjects(locale, *ids)

    override suspend fun getEpisodesBySeriesId(
        locale: String,
        id: String,
        original: Boolean?
    ): Array<BrowseObject> {
        val browseObjects = mutableListOf<BrowseObject>()

        val variantObjects = getSeasonsBySeriesId(locale, id)
            .flatMap { season ->
                getEpisodesBySeasonId(locale, season.id)
                    .onEach { episode -> browseObjects.add(episode.convertToBrowseObject()) }
                    .flatMap { it.getVariants(original) }
            }
            .subtract(browseObjects.map { it.id })
            .chunked(CRUNCHYROLL_CHUNK)
            .flatMap { chunk -> getObjects(locale, *chunk.toTypedArray()) }

        return (browseObjects + variantObjects).toTypedArray()
    }
}