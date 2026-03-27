package fr.shikkanime

import com.google.gson.JsonArray
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.system.exitProcess

suspend fun main() {
    val httpRequest = HttpRequest()
//    val response = httpRequest.get("https://www.france.tv/series-et-fictions/series-animees/goldorak-u")
    val response = httpRequest.get("https://www.france.tv/france-3/wakfu/")
    val body = response.bodyAsText()
    require(response.status == HttpStatusCode.OK) { "Failed to fetch page: ${response.status} - $body" }
    val parsedJson = requireNotNull("(?:3c|3e):(\\[.*?\\])\\\\n".toRegex().find(body)?.groupValues?.get(1)).replace("\\\"", "\"")
    println("Parsed JSON String: $parsedJson")
    val parsedJsonArray = ObjectParser.fromJson(parsedJson, JsonArray::class.java).flatMap { it.asJsonArray }
    println("Parsed JSON Array: $parsedJsonArray")

    var seasonName: String
    for (jsonElement in parsedJsonArray) {
        if (jsonElement.isJsonPrimitive) {
            seasonName = jsonElement.asJsonPrimitive.asString
            continue
        }

        println(jsonElement.asJsonObject)
    }

    exitProcess(0)
}