package fr.shikkanime

import com.google.gson.JsonArray
import fr.shikkanime.services.AnimeService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.CrunchyrollWrapper
import io.ktor.client.statement.*
import kotlin.system.exitProcess

suspend fun main() {
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val httpRequest = HttpRequest()
    val crunchyrollToken = CrunchyrollWrapper.getAnonymousAccessToken()

    animeService.findAll()
        .shuffled()
        .take(20)
        .forEach { anime ->
            val shortName = StringUtils.getShortName(anime.name!!)
            val platformIds: MutableSet<Pair<String, String>> = mutableSetOf()

            // https://myanimelist.net/search/prefix.json?type=anime&keyword=Reborn as a Vending Machine, I Now Wander the Dungeon&v=1
            val malResponse = httpRequest.get("https://myanimelist.net/search/prefix.json?type=anime&keyword=$shortName&v=1")

            ObjectParser.fromJson(malResponse.bodyAsText())
                .getAsJsonArray("categories")
                .first()
                .asJsonObject
                .getAsJsonArray("items")
                .forEach { item ->
                    val esScore = item.asJsonObject["es_score"].asDouble
                    if (esScore < 1.7) return@forEach
                    val id = item.asJsonObject["id"].asString
                    platformIds.add(Pair("MyAnimeList", id))
                }

            // https://api-v2.hyakanime.fr/explore?search=Reborn as a Vending Machine, I Now Wander the Dungeon&page=1
            val hyakanimeResponse = httpRequest.get("https://api-v2.hyakanime.fr/explore?search=$shortName&page=1")

            ObjectParser.fromJson(hyakanimeResponse.bodyAsText(), JsonArray::class.java)
                .forEach { item ->
                    val id = item.asJsonObject["id"].asString
                    platformIds.add(Pair("Hyakanime", id))
                }

            // https://www.crunchyroll.com/content/v2/discover/search?q=Reborn as a Vending Machine, I Now Wander the Dungeon&n=6&type=series&ratings=true&locale=fr-FR
//            val crunchyrollResponse = httpRequest.get("https://www.crunchyroll.com/content/v2/discover/search?q=$shortName&n=6&type=series&ratings=true&locale=fr-FR", mapOf("Authorization" to "Bearer $crunchyrollToken"))
//
//            val crunchyrollItems = ObjectParser.fromJson(crunchyrollResponse.bodyAsText())
//                .getAsJsonArray("data")
//                .first()
//                .asJsonObject
//                .getAsJsonArray("items")
//
//            crunchyrollItems.forEach { item ->
//                    val searchMetadata = item.asJsonObject["search_metadata"].asJsonObject["score"].asDouble
//                    if (crunchyrollItems.size() > 1 && searchMetadata < 800.0) return@forEach
//                    val id = item.asJsonObject["id"].asString
//                    platformIds.add(Pair("Crunchyroll", id))
//                }

            // https://adkami.com/ajax/search/quick
//            val adkamiResponse = httpRequest.post("https://adkami.com/ajax/search/quick", mapOf("Content-Type" to "application/x-www-form-urlencoded"), body = "query=$shortName")

            println("Anime: ${anime.name}")
            println("PlatformIds: $platformIds")
        }

    httpRequest.close()
    exitProcess(0)
}